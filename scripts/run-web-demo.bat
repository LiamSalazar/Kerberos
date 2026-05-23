@echo off
setlocal
cd /d "%~dp0..\auth-web-demo"

where npm.cmd >nul 2>nul
if %ERRORLEVEL%==0 (
    npm.cmd run dev
    exit /b %ERRORLEVEL%
)

set "NODE_HOME="
for /d %%D in ("D:\Kerberos\tools\node-*-win-x64") do set "NODE_HOME=%%~fD"

if defined NODE_HOME (
    set "PATH=%NODE_HOME%;%PATH%"
    "%NODE_HOME%\npm.cmd" run dev
    exit /b %ERRORLEVEL%
)

echo npm.cmd no esta disponible. Instala Node.js y npm, o agrega D:\Kerberos\tools\node-*-win-x64 al PATH.
exit /b 1
