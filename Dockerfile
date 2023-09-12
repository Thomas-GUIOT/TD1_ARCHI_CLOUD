# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN ./mvnw -B dependency:go-offline
COPY ./src ./src

RUN ./mvnw -B verify

EXPOSE 8080

CMD ["./mvnw", "exec:java"]