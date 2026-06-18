FROM eclipse-temurin:25
WORKDIR /app
COPY frontend.build/libs/frontend-0.1.2.jar frontend.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "frontend.jar"]
