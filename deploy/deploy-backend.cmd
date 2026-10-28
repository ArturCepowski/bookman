@echo off
rem Runs deploy-backend.ps1 from cmd.exe (a .ps1 file started from cmd only opens in Notepad).
rem Usage, from the repository root:  deploy\deploy-backend.cmd -Key C:\Users\YOU\.ssh\bookman-oracle.key
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0deploy-backend.ps1" %*
exit /b %ERRORLEVEL%
