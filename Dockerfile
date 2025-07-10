FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.7.1_3.2.0 AS builder

WORKDIR /app

COPY project project
COPY *.sbt .

RUN sbt update

COPY 01-abstractions 01-abstractions
COPY 02-services 02-services
COPY 03-api 03-api
COPY 04-app 04-app
COPY *.conf .

RUN sbt "project app" Universal/stage

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=builder /app/04-app/target/universal/stage /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    postgresql-client \
    && rm -rf /var/lib/apt/lists/*

COPY scripts/init-admin.sh /app/init-admin.sh
COPY scripts/JBCryptHasher.java /app/scripts/JBCryptHasher.java
RUN chmod +x /app/init-admin.sh

EXPOSE 8080

ENV DB_HOST=postgres \
  DB_PORT=5432 \
  DB_NAME=bank \
  DB_USER=postgres \
  DB_PASSWORD=postgres

RUN chmod +x /app/bin/app

RUN cat << EOF > /app/run.sh
#!/bin/bash
set -e

# Ждем готовности базы данных
until pg_isready -h \$DB_HOST -p \$DB_PORT -U \$DB_USER; do
    echo "Ожидаем готовности базы данных..."
    sleep 2
done

# Запускаем приложение в фоновом режиме для выполнения миграций
/app/bin/app &

# Ожидаем создания таблицы app_users (индикатор завершения миграций)
until PGPASSWORD=\$DB_PASSWORD psql -h \$DB_HOST -p \$DB_PORT -U \$DB_USER -d \$DB_NAME -tAc "SELECT 1 FROM pg_tables WHERE tablename = 'app_users';" | grep -q 1; do
    echo "Ожидаем создания таблицы 'app_users'..."
    sleep 2
done

# Таблица app_users существует. Запускаем скрипт инициализации администратора.
/app/init-admin.sh

echo "Переводим приложение на передний план..."
wait # Ждем завершения фонового процесса приложения
EOF
CMD ["/bin/bash", "/app/run.sh"] 