FROM hub.c.163.com/patest/openjdk-node:11-jdk

WORKDIR /app/mycov
COPY build/libs/*.jar ./

CMD ["java", "-jar", "/app/mycov/mycov-1.0.jar"]

EXPOSE 8080
