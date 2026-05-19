@echo off
setlocal
cd /d "%~dp0.."
call mvn -q -DskipTests compile
if errorlevel 1 exit /b %errorlevel%
set "AUTH_CP=auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes;auth-as\target\classes;auth-tgs\target\classes;auth-service\target\classes;auth-client-sdk\target\classes"
java -cp "%AUTH_CP%" com.portfolio.auth.client.ClientCli %*
