#!/bin/bash

# Скрипт для создания первого администратора
# Использует переменные окружения для безопасной настройки

set -e

if [ -z "$ADMIN_EMAIL" ]; then
    echo "ADMIN_EMAIL не установлен, используем значение по умолчанию: admin@bank.com"
    ADMIN_EMAIL="admin@bank.com"
fi

if [ -z "$ADMIN_PASSWORD" ]; then
    echo "ADMIN_PASSWORD не установлен, используем значение по умолчанию: admin123"
    ADMIN_PASSWORD="admin123"
fi

if [ -z "$ADMIN_FIRST_NAME" ]; then
    ADMIN_FIRST_NAME="Admin"
fi

if [ -z "$ADMIN_LAST_NAME" ]; then
    ADMIN_LAST_NAME="User"
fi

echo "Проверяем подключение к базе данных..."
until pg_isready -h $DB_HOST -p $DB_PORT -U $DB_USER; do
    echo "Ожидаем готовности базы данных..."
    sleep 2
done

echo "База данных готова. Начинаем инициализацию..."

echo "Компилируем Java-хешер..."
# Убедимся, что javac доступен и имеет доступ к jbcrypt JAR
# CP_PATH содержит путь к jbcrypt JAR, полученный ранее
CP_PATH="/app/lib/org.mindrot.jbcrypt-0.4.jar"


javac -cp "$CP_PATH" /app/scripts/JBCryptHasher.java

if [ $? -ne 0 ]; then
  echo "Ошибка при компиляции Java-хешера."
  exit 1
fi

echo "Генерируем хеш пароля..."
# Используем скомпилированный класс
PASSWORD_HASH=$(java -cp "/app/scripts/:$CP_PATH" JBCryptHasher "$ADMIN_PASSWORD")

if [ $? -ne 0 ]; then
  echo "Ошибка при генерации хеша пароля."
  exit 1
fi

# Удаляем скомпилированный класс после использования
rm /app/scripts/JBCryptHasher.class

echo "Создаем администратора: $ADMIN_EMAIL"

PGPASSWORD=$DB_PASSWORD psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME << EOF
INSERT INTO app_users (id, email, password_hash, first_name, last_name, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '$ADMIN_EMAIL',
    '$PASSWORD_HASH',
    '$ADMIN_FIRST_NAME',
    '$ADMIN_LAST_NAME',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    is_active = EXCLUDED.is_active,
    updated_at = NOW();

-- Назначаем роль ADMIN только если пользователь создан и у него еще нет этой роли
INSERT INTO app_user_roles (user_id, role_id)
SELECT
    u.id,
    r.id
FROM app_users u
CROSS JOIN app_roles r
WHERE u.email = '$ADMIN_EMAIL'
  AND r.name = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM app_user_roles ur
    WHERE ur.user_id = u.id AND ur.role_id = r.id
  );
EOF

if [ $? -eq 0 ]; then
    echo "Администратор успешно создан/обновлен!"
    echo "Email: $ADMIN_EMAIL"
    echo "Роль: ADMIN"
else
    echo "Ошибка при создании администратора"
    exit 1
fi 