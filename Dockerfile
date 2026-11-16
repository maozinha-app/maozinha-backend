# syntax=docker/dockerfile:1

# ---------- Etapa 1: build com Maven + JDK 21 ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src

# O cache do ~/.m2 acelera rebuilds sem ficar gravado na imagem.
# Os testes não rodam aqui porque o contextLoads precisa de um banco disponível.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

# ---------- Etapa 2: runtime só com o JRE ----------
FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S maozinha && adduser -S maozinha -G maozinha
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar

USER maozinha
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
