FROM hub.c.163.com/patest/openjdk-node:jdk

# Copy source codes to container.
WORKDIR /app/mycov
COPY build/libs/*.jar ./
RUN bash -c "[ -e *.jar ]"

ENV JAR_FILE /app/mycov/mycov-1.0.jar

EXPOSE 8080