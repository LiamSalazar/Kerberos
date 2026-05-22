#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
mvn -q -pl auth-storage-sqlite -am -DskipTests package org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies -DoutputDirectory=target/dependency -DincludeScope=runtime

AUTH_CP="auth-storage-sqlite/target/classes:auth-storage-sqlite/target/dependency/*"
exec java -cp "$AUTH_CP" com.portfolio.auth.storage.sqlite.SQLiteAdminCli "$@"
