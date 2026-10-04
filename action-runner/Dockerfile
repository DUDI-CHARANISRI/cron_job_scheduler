FROM maven:3.9.4-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY action-runner/pom.xml action-runner/pom.xml
COPY action-runner/src action-runner/src
RUN mvn -f action-runner/pom.xml -DskipTests package -DskipTests -q

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/action-runner/target/action-runner-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java","-jar","/app/app.jar"]
