FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:25-jre
COPY --from=build /workspace/target/*.jar /app.jar
USER 10001
ENTRYPOINT ["java", "-jar", "/app.jar"]
