' Create Desktop Shortcut for Tire Shop POS
' Run this script once to create the shortcut

Set WshShell = CreateObject("WScript.Shell")
Set FSO = CreateObject("Scripting.FileSystemObject")

' Get paths
strDesktop = WshShell.SpecialFolders("Desktop")
strScriptPath = FSO.GetParentFolderName(WScript.ScriptFullName)
strVbsPath = strScriptPath & "\start-tireshop-hidden.vbs"
strIconPath = strScriptPath & "\tireshop.ico"

' Create the shortcut - points to wscript.exe running our VBS
Set oShortcut = WshShell.CreateShortcut(strDesktop & "\Tire Shop POS.lnk")
oShortcut.TargetPath = "wscript.exe"
oShortcut.Arguments = """" & strVbsPath & """"
oShortcut.WorkingDirectory = strScriptPath
oShortcut.Description = "Start Tire Shop POS System"
oShortcut.WindowStyle = 1

' Use custom icon if exists, otherwise use a Windows system icon
If FSO.FileExists(strIconPath) Then
    oShortcut.IconLocation = strIconPath & ",0"
Else
    ' Use shell32.dll icon 15 (computer icon) as fallback - good for POS
    oShortcut.IconLocation = "%SystemRoot%\System32\shell32.dll,15"
End If

oShortcut.Save

MsgBox "Desktop shortcut created successfully!" & vbCrLf & vbCrLf & _
       "Look for 'Tire Shop POS' on your desktop." & vbCrLf & vbCrLf & _
       "Tip: To use a custom icon, place 'tireshop.ico' in:" & vbCrLf & _
       strScriptPath, vbInformation, "Tire Shop POS"
