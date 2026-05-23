@echo off
setlocal
cd /d "%~dp0.."
call mvn -q -pl auth-storage-sqlite -am -DskipTests package org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies "-DoutputDirectory=target/dependency" "-DincludeScope=runtime"
if errorlevel 1 exit /b %errorlevel%
set "AUTH_CP=auth-storage-sqlite\target\classes;auth-storage-sqlite\target\dependency\*"
java -cp "%AUTH_CP%" com.portfolio.auth.storage.sqlite.SQLiteAdminCli %*
