FROM eclipse-temurin:21-jre
WORKDIR /app
COPY strangler-gateway/target/strangler-gateway-1.0.0.jar app.jar
USER 10001
EXPOSE 8082
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
