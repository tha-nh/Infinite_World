FROM eclipse-temurin:21-jre

WORKDIR /app

COPY i18n/target/i18n-service-1.0.0-SNAPSHOT.jar app.jar
COPY docker/libs/opentelemetry-javaagent_1.30.0.jar opentelemetry-javaagent.jar


ENTRYPOINT ["java","--enable-preview","-jar","/app/app.jar"]
