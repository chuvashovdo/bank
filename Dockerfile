FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 AS builder

WORKDIR /app

COPY project project
COPY *.sbt .
COPY *.conf .

RUN sbt update

COPY 01-abstractions 01-abstractions
COPY 02-services 02-services
COPY 03-api 03-api
COPY 04-app 04-app

RUN sbt "project app" Universal/stage

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/04-app/target/universal/stage /app

EXPOSE 8080

ENV DB_HOST=postgres \
  DB_PORT=5432 \
  DB_NAME=bank \
  DB_USER=postgres \
  DB_PASSWORD=postgres

RUN chmod +x /app/bin/app

CMD ["/app/bin/app"] 