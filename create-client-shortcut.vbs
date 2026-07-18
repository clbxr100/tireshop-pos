' Create Desktop Shortcut for Tire Shop POS (Client)
' Run this script on the CLIENT computer

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

' Get paths
strDesktop = WshShell.SpecialFolders("Desktop")
strScriptPath = FSO.GetParentFolderName(WScript.ScriptFullName)
strVbsPath = strScriptPath & "\start-tireshop-client.vbs"
strIconPath = strScriptPath & "\tireshop.ico"

' Create the shortcut
Set oShortcut = WshShell.CreateShortcut(strDesktop & "\Tire Shop POS.lnk")
oShortcut.TargetPath = "wscript.exe"
oShortcut.Arguments = """" & strVbsPath & """"
oShortcut.WorkingDirectory = strScriptPath
oShortcut.Description = "Start Tire Shop POS (Client)"
oShortcut.WindowStyle = 1

' Use custom icon if exists, otherwise use network computer icon
If FSO.FileExists(strIconPath) Then
    oShortcut.IconLocation = strIconPath & ",0"
Else
    ' Use shell32.dll icon 17 (network computer) for client
    oShortcut.IconLocation = "%SystemRoot%\System32\shell32.dll,17"
End If

oShortcut.Save

' Ask for server IP configuration
strCurrentIP = "192.168.1.100"

' Read current IP from the VBS file
strVbsContent = ""
Set vbsFile = FSO.OpenTextFile(strVbsPath, 1)
strVbsContent = vbsFile.ReadAll()
vbsFile.Close

' Extract current IP
intStart = InStr(strVbsContent, "strServerIP = """) + 15
intEnd = InStr(intStart, strVbsContent, """")
If intStart > 15 And intEnd > intStart Then
    strCurrentIP = Mid(strVbsContent, intStart, intEnd - intStart)
End If

strNewIP = InputBox("Enter the SERVER computer's IP address:" & vbCrLf & vbCrLf & _
                    "(This is the IP of the computer running the database)" & vbCrLf & vbCrLf & _
                    "To find it, run 'ipconfig' on the server computer.", _
                    "Server Configuration", strCurrentIP)

If strNewIP <> "" And strNewIP <> strCurrentIP Then
    ' Update the VBS file with new IP
    strVbsContent = Replace(strVbsContent, "strServerIP = """ & strCurrentIP & """", "strServerIP = """ & strNewIP & """")

    Set vbsFile = FSO.CreateTextFile(strVbsPath, True)
    vbsFile.Write strVbsContent
    vbsFile.Close

    ' Also update database.properties if it exists
    strDbProps = strScriptPath & "\database.properties"
    If FSO.FileExists(strDbProps) Then
        FSO.DeleteFile strDbProps, True
    End If

    MsgBox "Configuration updated!" & vbCrLf & vbCrLf & _
           "Server IP: " & strNewIP & vbCrLf & vbCrLf & _
           "Desktop shortcut created: 'Tire Shop POS'", vbInformation, "Tire Shop POS - Client Setup"
Else
    MsgBox "Desktop shortcut created!" & vbCrLf & vbCrLf & _
           "Server IP: " & strCurrentIP & vbCrLf & vbCrLf & _
           "Look for 'Tire Shop POS' on your desktop.", vbInformation, "Tire Shop POS - Client Setup"
End If
