FROM openjdk:21-jdk-slim
VOLUME /tmp
COPY services/svc-ledger/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
