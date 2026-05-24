FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl auth-websocket-gateway -am -DskipTests install \
    && mvn -q -pl auth-websocket-gateway -DskipTests org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies \
       -DincludeScope=runtime \
       -DoutputDirectory=/workspace/auth-websocket-gateway/target/dependency

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-websocket-gateway/target/classes /app/classes
COPY --from=build /workspace/auth-websocket-gateway/target/dependency /app/dependency
COPY --from=build /workspace/scripts/sqlite/migrations /app/scripts/sqlite/migrations
COPY --from=build /workspace/scripts/postgres/migrations /app/scripts/postgres/migrations
ENTRYPOINT ["java", "-cp", "/app/classes:/app/dependency/*", "com.portfolio.auth.gateway.WebSocketGatewayApp"]
