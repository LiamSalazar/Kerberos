#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
mvn -q -pl auth-websocket-gateway -am -DskipTests compile org.apache.maven.plugins:maven-dependency-plugin:3.6.1:build-classpath "-Dmdep.outputFile=target/runtime-classpath.txt" "-Dmdep.excludeScope=test"

AUTH_WS_DEPS="$(cat auth-websocket-gateway/target/runtime-classpath.txt)"
AUTH_CP="auth-core/target/classes:auth-crypto/target/classes:auth-transport/target/classes:auth-as/target/classes:auth-tgs/target/classes:auth-service/target/classes:auth-client-sdk/target/classes:auth-websocket-gateway/target/classes:$AUTH_WS_DEPS"
exec java -cp "$AUTH_CP" com.portfolio.auth.gateway.WebSocketGatewayApp "$@"
