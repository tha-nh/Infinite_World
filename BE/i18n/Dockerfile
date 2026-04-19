FROM eclipse-temurin:21-jre

WORKDIR /app

COPY i18n-service/target/i18n-0.0.1-SNAPSHOT.jar app.jar
COPY docker/libs/opentelemetry-javaagent_1.30.0.jar opentelemetry-javaagent.jar
COPY deployment/app-config /app-config

ENTRYPOINT ["java","--enable-preview","-javaagent:/app/opentelemetry-javaagent.jar","-jar","/app/app.jar"]
