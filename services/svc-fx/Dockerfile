FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY services/svc-fx/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
