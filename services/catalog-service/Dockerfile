FROM eclipse-temurin:21-jre
WORKDIR /app
COPY services/catalog-service/target/catalog-service-1.0.0.jar app.jar
USER 10001
EXPOSE 8083
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
