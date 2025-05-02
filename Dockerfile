FROM eclipse-temurin:17-jdk AS builder

WORKDIR /app

# Установка SBT
RUN apt-get update && \
  apt-get install -y curl && \
  curl -L -o sbt.deb https://repo.scala-sbt.org/scalasbt/debian/sbt-1.9.8.deb && \
  dpkg -i sbt.deb && \
  apt-get update && \
  apt-get install -y sbt && \
  rm sbt.deb && \
  apt-get clean && \
  rm -rf /var/lib/apt/lists/*

# Копируем только файлы SBT сначала, чтобы кэшировать зависимости
COPY project project
COPY *.sbt .
COPY *.conf .

# Кэшируем зависимости
RUN sbt update

# Копируем исходный код
COPY 01-abstractions 01-abstractions
COPY 02-services 02-services
COPY 03-app 03-app

# Собираем проект
RUN sbt "project main" Universal/stage

# Финальный образ - используем обычный Debian-based образ вместо Alpine
FROM eclipse-temurin:17-jre

WORKDIR /app

# Копируем собранное приложение
COPY --from=builder /app/03-app/target/universal/stage /app

# Экспонируем порт
EXPOSE 8080

# Переменные окружения для подключения к БД
ENV DB_HOST=postgres \
  DB_PORT=5432 \
  DB_NAME=bank \
  DB_USER=postgres \
  DB_PASSWORD=postgres

# Делаем скрипты исполняемыми
RUN chmod +x /app/bin/main

# Запускаем приложение
CMD ["/app/bin/main"] 