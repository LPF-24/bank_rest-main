FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY . .
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app

COPY --chown=app:app --from=builder /app/target/*.jar /app/app.jar

USER app

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
