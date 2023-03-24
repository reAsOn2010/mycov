FROM adoptopenjdk:17-jre

WORKDIR /app/mycov
COPY build/libs/*.jar ./

CMD ["java", "-jar", "/app/mycov/mycov-1.0.jar"]

EXPOSE 8080
