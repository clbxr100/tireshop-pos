' Tire Shop POS - Client Launcher
' Connects to the main computer's database server
' Only the POS application window will be visible

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

' Get the script directory
strPath = FSO.GetParentFolderName(WScript.ScriptFullName)

' ============================================
' CONFIGURATION - CHANGE THIS!
' ============================================
strServerIP = "192.168.1.100"

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
        ' Also kill TireRack service
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
    If DateDiff("s", lockFile.DateLastModified, Now) < 60 Then
        MsgBox "Tire Shop POS is starting up..." & vbCrLf & vbCrLf & _
               "Please wait for the application to appear.", vbInformation, "Tire Shop POS"
        WScript.Quit
    Else
        FSO.DeleteFile strLockFile, True
    End If
End If

' Create lock file
Set lockFileWrite = FSO.CreateTextFile(strLockFile, True)
lockFileWrite.WriteLine "POS Client Starting: " & Now
lockFileWrite.Close

' ============================================
' SHOW STARTUP MESSAGE
' ============================================
WshShell.Popup "Starting Tire Shop POS (Client Mode)..." & vbCrLf & vbCrLf & _
               "Connecting to server: " & strServerIP, 3, "Tire Shop POS", 64

' Change to the project directory
WshShell.CurrentDirectory = strPath

' Set up environment variables
strJavaHome = strPath & "\jdk-21.0.9+10"
strMavenHome = strPath & "\apache-maven-3.9.6"
strJavaExe = strJavaHome & "\bin\java.exe"

Set objEnv = WshShell.Environment("PROCESS")
objEnv("JAVA_HOME") = strJavaHome
objEnv("PATH") = strJavaHome & "\bin;" & strMavenHome & "\bin;" & objEnv("PATH")

' ============================================
' CHECK SERVER CONNECTION
' ============================================
Set objExec = WshShell.Exec("cmd /c ping -n 1 " & strServerIP)
strOutput = objExec.StdOut.ReadAll()

If InStr(strOutput, "Reply from") = 0 Then
    FSO.DeleteFile strLockFile, True
    MsgBox "Cannot reach database server at " & strServerIP & "!" & vbCrLf & vbCrLf & _
           "Please check:" & vbCrLf & _
           "1. The server computer is turned on" & vbCrLf & _
           "2. The database server is running (start-tireshop.bat)" & vbCrLf & _
           "3. Both computers are on the same network" & vbCrLf & vbCrLf & _
           "To change the server IP, edit:" & vbCrLf & _
           strPath & "\start-tireshop-client.vbs", vbCritical, "Connection Error"
    WScript.Quit
End If

' ============================================
' CREATE DATABASE CONFIG (if needed)
' ============================================
strDbConfig = strPath & "\database.properties"
If Not FSO.FileExists(strDbConfig) Then
    Set configFile = FSO.CreateTextFile(strDbConfig, True)
    configFile.WriteLine "# Network database configuration"
    configFile.WriteLine "db.url=jdbc:h2:tcp://" & strServerIP & ":9092/./tireshop"
    configFile.WriteLine "db.user=sa"
    configFile.WriteLine "db.password="
    configFile.Close
End If

' ============================================
' START SERVICES
' ============================================

' Start TireRack service (completely hidden)
strTireRackPath = strPath & "\tirerack-service"
If FSO.FolderExists(strTireRackPath) Then
    WshShell.Run "cmd /c cd /d """ & strTireRackPath & """ && node index.js", 0, False
    WScript.Sleep 2000
End If

' Update lock file
Set lockFileWrite = FSO.CreateTextFile(strLockFile, True)
lockFileWrite.WriteLine "POS Client Running: " & Now
lockFileWrite.Close

' Start the main POS application (hidden console, visible JavaFX window)
WshShell.Run "cmd /c cd /d """ & strPath & """ && mvn javafx:run -q", 0, True

' ============================================
' CLEANUP
' ============================================
On Error Resume Next

' Kill TireRack node service
Set colNode = objWMI.ExecQuery("SELECT * FROM Win32_Process WHERE CommandLine LIKE '%tirerack-service%'")
For Each objProcess In colNode
    objProcess.Terminate()
Next

' Remove lock file
FSO.DeleteFile strLockFile, True

On Error Goto 0
