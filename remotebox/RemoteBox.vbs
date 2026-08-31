' Lanceur silencieux de RemoteBox (pas de fenetre console).
' Construit la distribution Gradle au premier lancement, puis demarre l'app via javaw.
Option Explicit
Dim fso, sh, base, libDir, javaw, jh
Set fso = CreateObject("Scripting.FileSystemObject")
Set sh  = CreateObject("WScript.Shell")

base   = fso.GetParentFolderName(WScript.ScriptFullName)
libDir = base & "\build\install\remotebox\lib"

If Not fso.FolderExists(libDir) Then
    sh.CurrentDirectory = base
    ' visible pendant la construction initiale
    sh.Run "cmd /c "".\gradlew.bat installDist""", 1, True
End If

' Trouver javaw : JAVA_HOME sinon PATH
jh = sh.ExpandEnvironmentStrings("%JAVA_HOME%")
If jh <> "%JAVA_HOME%" And fso.FileExists(jh & "\bin\javaw.exe") Then
    javaw = """" & jh & "\bin\javaw.exe"""
Else
    javaw = "javaw.exe"
End If

sh.CurrentDirectory = base & "\build\install\remotebox"
sh.Run javaw & " -cp """ & libDir & "\*"" net.tawkit.remotebox.App", 0, False
