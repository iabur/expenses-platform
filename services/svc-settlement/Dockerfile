FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY services/svc-settlement/build/libs/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","/app/app.jar"]
