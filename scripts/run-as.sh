#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
mvn -q -pl auth-as -am -DskipTests package org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies -DoutputDirectory=target/dependency -DincludeScope=runtime

AUTH_CP="auth-as/target/classes:auth-as/target/dependency/*"
exec java -cp "$AUTH_CP" com.portfolio.auth.as.AuthenticationServerApp "$@"
