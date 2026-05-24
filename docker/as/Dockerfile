FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -pl auth-as -am -DskipTests package \
    org.apache.maven.plugins:maven-dependency-plugin:3.6.1:copy-dependencies \
    -DoutputDirectory=auth-as/target/dependency \
    -DincludeScope=runtime

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/auth-as/target/classes /app/classes
COPY --from=build /workspace/auth-as/target/dependency /app/dependency
COPY --from=build /workspace/scripts/sqlite/migrations /app/scripts/sqlite/migrations
COPY --from=build /workspace/scripts/postgres/migrations /app/scripts/postgres/migrations
ENTRYPOINT ["java", "-cp", "/app/classes:/app/dependency/*", "com.portfolio.auth.as.AuthenticationServerApp"]
