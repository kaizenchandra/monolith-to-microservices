FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/payment-service/target/payment-service-1.0.0.jar app.jar
USER 10001
EXPOSE 8086
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
