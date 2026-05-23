FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl auth-websocket-gateway -am -DskipTests package \
    org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies \
    -DoutputDirectory=auth-websocket-gateway/target/dependency \
    -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-websocket-gateway/target/classes /app/classes
COPY --from=build /workspace/auth-websocket-gateway/target/dependency /app/dependency
COPY --from=build /workspace/scripts/sqlite/migrations /app/scripts/sqlite/migrations
ENTRYPOINT ["java", "-cp", "/app/classes:/app/dependency/*", "com.portfolio.auth.gateway.WebSocketGatewayApp"]
