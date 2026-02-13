FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar /app/agentbot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/agentbot.jar"]
