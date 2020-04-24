FROM adoptopenjdk/openjdk8:slim

WORKDIR /app/mycov
COPY build/libs/*.jar ./

CMD ["java", "-jar", "/app/mycov/mycov-1.0.jar"]

EXPOSE 8080
