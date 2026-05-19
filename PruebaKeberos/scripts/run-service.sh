#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
mvn -q -DskipTests compile

AUTH_CP="auth-core/target/classes:auth-crypto/target/classes:auth-transport/target/classes:auth-as/target/classes:auth-tgs/target/classes:auth-service/target/classes:auth-client-sdk/target/classes"
exec java -cp "$AUTH_CP" com.portfolio.auth.service.ProtectedServiceApp "$@"
