FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/inventory-service/target/inventory-service-1.0.0.jar app.jar
USER 10001
EXPOSE 8084
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
