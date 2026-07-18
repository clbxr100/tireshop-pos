' Tire Shop POS Launcher
' Starts all services hidden and launches the POS application
' Only the POS application window will be visible

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

' Get the script directory
strPath = FSO.GetParentFolderName(WScript.ScriptFullName)

' ============================================
' PREVENT MULTIPLE INSTANCES
' ============================================
strLockFile = strPath & "\pos-running.lock"

' Check if already running by looking for our Java process
Set objWMI = GetObject("winmgmts:\\.\root\cimv2")
Set colProcesses = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%com.tireshop%' OR CommandLine LIKE '%javafx:run%'")

If colProcesses.Count > 0 Then
    intResponse = MsgBox("Tire Shop POS appears to be running!" & vbCrLf & vbCrLf & _
           "Click YES to force close and restart" & vbCrLf & _
           "Click NO to cancel", vbYesNo + vbExclamation, "Tire Shop POS")

    If intResponse = vbYes Then
        ' Force kill existing processes
        For Each objProcess In colProcesses
            objProcess.Terminate()
        Next
        ' Also kill related processes
        Set colH2 = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%org.h2.tools.Server%'")
        For Each objProcess In colH2
            objProcess.Terminate()
        Next
        Set colNode = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%tirerack-service%'")
        For Each objProcess In colNode
            objProcess.Terminate()
        Next
        ' Delete stale lock file
        If FSO.FileExists(strLockFile) Then
            FSO.DeleteFile strLockFile, True
        End If
        WScript.Sleep 2000
    Else
        WScript.Quit
    End If
End If

' Also check lock file for "just started" scenario (prevents double-click)
If FSO.FileExists(strLockFile) Then
    Set lockFile = FSO.GetFile(strLockFile)
    ' If lock file is less than 60 seconds old, app is still starting
    If DateDiff("s", lockFile.DateLastModified, Now) < 60 Then
        MsgBox "Tire Shop POS is starting up..." & vbCrLf & vbCrLf & _
               "Please wait for the application to appear.", vbInformation, "Tire Shop POS"
        WScript.Quit
    Else
        ' Old lock file from crash - delete it
        FSO.DeleteFile strLockFile, True
    End If
End If

' Create lock file
Set lockFileWrite = FSO.CreateTextFile(strLockFile, True)
lockFileWrite.WriteLine "POS Starting: " & Now
lockFileWrite.Close

' ============================================
' SHOW STARTUP MESSAGE
' ============================================
' Create a popup that auto-closes after 3 seconds
WshShell.Popup "Starting Tire Shop POS..." & vbCrLf & vbCrLf & _
               "Please wait while services start.", 3, "Tire Shop POS", 64

' Change to the project directory
WshShell.CurrentDirectory = strPath

' Set up environment variables
strJavaHome = strPath & "\jdk-21.0.9+10"
strMavenHome = strPath & "\apache-maven-3.9.6"
strJavaExe = strJavaHome & "\bin\java.exe"
strJavacExe = strJavaHome & "\bin\javac.exe"

' Set PATH for this session
Set objEnv = WshShell.Environment("PROCESS")
objEnv("JAVA_HOME") = strJavaHome
objEnv("PATH") = strJavaHome & "\bin;" & strMavenHome & "\bin;" & objEnv("PATH")

' H2 JAR path
strH2Jar = WshShell.ExpandEnvironmentStrings("%USERPROFILE%") & "\.m2\repository\com\h2database\h2\2.1.214\h2-2.1.214.jar"

' Check if database server is already running
Set objExec = WshShell.Exec("cmd /c netstat -an | findstr :9092 | findstr LISTENING")
strOutput = objExec.StdOut.ReadAll()
If Len(Trim(strOutput)) = 0 Then
    ' Start H2 Database Server (completely hidden)
    If FSO.FileExists(strH2Jar) Then
        strDbCmd = """" & strJavaExe & """ -cp """ & strH2Jar & """ org.h2.tools.Server -tcp -tcpAllowOthers -tcpPort 9092 -baseDir """ & strPath & """ -ifNotExists"
        WshShell.Run strDbCmd, 0, False
        WScript.Sleep 2000
    End If
End If

' Check if print server should be started
strPrintServerFlag = strPath & "\enable-print-server.txt"
If FSO.FileExists(strPrintServerFlag) Then
    ' Compile print server if needed
    strTempClasses = strPath & "\temp-classes"
    If Not FSO.FolderExists(strTempClasses) Then
        FSO.CreateFolder(strTempClasses)
    End If

    strPrintServerSrc = strPath & "\src\main\java\com\tireshop\util\SimplePrintServer.java"
    If FSO.FileExists(strPrintServerSrc) Then
        ' Compile (hidden)
        WshShell.Run """" & strJavacExe & """ -d """ & strTempClasses & """ """ & strPrintServerSrc & """", 0, True
        ' Start print server (hidden)
        WshShell.Run """" & strJavaExe & """ -cp """ & strTempClasses & """ com.tireshop.util.SimplePrintServer", 0, False
    End If
End If

' Start TireRack service (completely hidden)
strTireRackPath = strPath & "\tirerack-service"
If FSO.FolderExists(strTireRackPath) Then
    WshShell.Run "cmd /c cd /d """ & strTireRackPath & """ && node index.js", 0, False
    WScript.Sleep 2000
End If

' Update lock file to show app is now running
Set lockFileWrite = FSO.CreateTextFile(strLockFile, True)
lockFileWrite.WriteLine "POS Running: " & Now
lockFileWrite.Close

' Start the main POS application
' Run mvn hidden - the JavaFX window will appear on its own
WshShell.Run "cmd /c cd /d """ & strPath & """ && mvn javafx:run -q", 0, True

' When POS closes, cleanup background services
On Error Resume Next

' Re-get WMI object for cleanup
Set objWMI = GetObject("winmgmts:\\.\root\cimv2")

' Kill H2 database server
Set colH2 = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%org.h2.tools.Server%'")
For Each objProcess In colH2
    objProcess.Terminate()
Next

' Kill print server
Set colPrint = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%SimplePrintServer%'")
For Each objProcess In colPrint
    objProcess.Terminate()
Next

' Kill TireRack node service
Set colNode = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%tirerack-service%'")
For Each objProcess In colNode
    objProcess.Terminate()
Next

' Fallback: use taskkill for any remaining processes
WshShell.Run "cmd /c taskkill /F /FI ""WINDOWTITLE eq Database Server*"" 2>nul", 0, True
WshShell.Run "cmd /c taskkill /F /FI ""WINDOWTITLE eq Print Server*"" 2>nul", 0, True
WshShell.Run "cmd /c taskkill /F /FI ""WINDOWTITLE eq TireRack*"" 2>nul", 0, True

' Remove lock file
FSO.DeleteFile strLockFile, True

On Error Goto 0
