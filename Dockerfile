#FROM openjdk:11-jdk-slim AS build-env
FROM maven:3.6.3 AS build-env
ADD . /app
WORKDIR /app
RUN mvn package

FROM gcr.io/distroless/java:11
COPY --from=build-env /app/target/icestreamer-1.0-SNAPSHOT.jar /app/
WORKDIR /app
CMD ["icestreamer-1.0-SNAPSHOT.jar"]

