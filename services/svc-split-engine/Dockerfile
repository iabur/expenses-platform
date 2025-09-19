FROM openjdk:21-jdk-slim

WORKDIR /app

COPY services/svc-split-engine/build/libs/svc-split-engine-*.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
