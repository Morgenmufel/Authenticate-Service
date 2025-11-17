FROM eclipse-temurin:21-jdk-slim AS build
WORKDIR /app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
RUN ./mvnw -Dmaven.test.skip=true clean package
FROM eclipse-temurin:21-jre-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
