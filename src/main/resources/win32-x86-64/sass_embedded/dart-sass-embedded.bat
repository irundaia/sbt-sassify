@echo off
REM This script drives the standalone sass_embedded package, which bundles together a
REM Dart executable and a snapshot of sass_embedded.

set SCRIPTPATH=%~dp0
set arguments=%*
"%SCRIPTPATH%\src\dart.exe" "%SCRIPTPATH%\src\dart-sass-embedded.snapshot" %arguments%
