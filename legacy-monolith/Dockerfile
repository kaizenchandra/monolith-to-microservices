FROM eclipse-temurin:21-jre
WORKDIR /app
COPY legacy-monolith/target/legacy-monolith-1.0.0.jar app.jar
USER 10001
EXPOSE 8081
ENTRYPOINT ["java","-XX:MaxRAMPercentage=70","-jar","app.jar"]
