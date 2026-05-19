@echo off
setlocal
cd /d "%~dp0.."
call mvn -q -pl auth-websocket-gateway -am -DskipTests compile org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath "-Dmdep.outputFile=target/runtime-classpath.txt" "-Dmdep.excludeScope=test"
if errorlevel 1 exit /b %errorlevel%
set /p AUTH_WS_DEPS=<auth-websocket-gateway\target\runtime-classpath.txt
set "AUTH_CP=auth-core\target\classes;auth-crypto\target\classes;auth-transport\target\classes;auth-as\target\classes;auth-tgs\target\classes;auth-service\target\classes;auth-client-sdk\target\classes;auth-websocket-gateway\target\classes;%AUTH_WS_DEPS%"
java -cp "%AUTH_CP%" com.portfolio.auth.gateway.WebSocketGatewayApp %*
