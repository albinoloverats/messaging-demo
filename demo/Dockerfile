FROM eclipse-temurin:25
WORKDIR /app
COPY build/libs/demo-0.1.1.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
