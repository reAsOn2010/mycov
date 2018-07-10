FROM openjdk:8-jdk-alpine

WORKDIR /app/mycov
COPY build/libs/*.jar ./

CMD ["java", "-jar", "/app/mycov/mycov-1.0.jar"]

EXPOSE 8080
