@echo off
setlocal
cd /d "%~dp0.."
if "%AUTH_SAMPLE_LOGIN_PORT%"=="" set "AUTH_SAMPLE_LOGIN_PORT=5174"

where py >nul 2>nul
if %ERRORLEVEL%==0 (
    echo sample-login-app escuchando en http://127.0.0.1:%AUTH_SAMPLE_LOGIN_PORT%
    py -3 -m http.server %AUTH_SAMPLE_LOGIN_PORT% --bind 127.0.0.1 --directory sample-login-app
    exit /b %ERRORLEVEL%
)

where python >nul 2>nul
if %ERRORLEVEL%==0 (
    echo sample-login-app escuchando en http://127.0.0.1:%AUTH_SAMPLE_LOGIN_PORT%
    python -m http.server %AUTH_SAMPLE_LOGIN_PORT% --bind 127.0.0.1 --directory sample-login-app
    exit /b %ERRORLEVEL%
)

echo Python no esta disponible para servir sample-login-app.
exit /b 1
