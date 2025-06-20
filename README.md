# Банковское приложение

Backend система, построенная с использованием функционального программирования на Scala 3, для управления банковскими аккаунтами, транзакциями и аутентификацией пользователей.

## Технологии

- **ZIO** - функциональная библиотека для описания асинхронного и параллельного программирования и работы с эффектами
- **Tapir** - библиотека для типобезопасного описания HTTP API с автогенерацией Swagger
- **PostgreSQL** - реляционная база данных
- **Quill** - библиотека для типобезопасных SQL-запросов
- **Flyway** - миграция баз данных
- **JWT** - для безопасной аутентификации
- **Iron** - для создания уточненных типов (refined types) и повышения типобезопасности моделей
- **jBCrypt** - для хеширования паролей
- **Docker** - контейнеризация приложения
- **sbt** - система сборки Scala-проектов

## Архитектура

Проект разделен на несколько модулей в соответствии с принципами чистой архитектуры и DDD:

- **01-abstractions**: Определяет основные доменные модели (сущности, кастомные типы данных, бизнес-ошибки) и абстракции сервисов (интерфейсы). Этот модуль не зависит от конкретных реализаций.
- **02-services**: Содержит реализации бизнес-логики. Каждый сервис является отдельным проектом sbt:
  - **common-service**: Общие компоненты, используемые другими сервисами, например, `Transactor` для управления транзакциями БД и конфигурация Flyway.
  - **auth-service**: Реализация логики аутентификации.
  - **user-service**: Реализация логики управления пользователями (регистрация, обновление профиля, смена пароля).
  - **jwt-service**: Реализация логики создания и валидации JWT-токенов.
  - **bank-service**: Бизнес-логика для банковских операций, включая управление счетами и транзакциями.
- **03-api**: Отвечает за определение HTTP API эндпоинтов с использованием Tapir. Зависит от сервисных абстракций и доменных моделей.
- **04-app**: Точка входа приложения. Собирает вместе все компоненты (слои ZIO для сервисов, API, конфигурацию) и запускает HTTP сервер.

Всего в `build.sbt` сконфигурировано 8 основных проектов: `abstractions`, `commonService`, `authService`, `userService`, `jwtService`, `bankService`, `httpApi` и `app`.

Архитектура построена на принципах чистой функциональной архитектуры с использованием **ZIO Environment** для внедрения зависимостей (DI). Приложение следует подходу **порт-адаптер**, где бизнес-логика (сервисы) изолирована от внешних влияний (HTTP, база данных).

## API-эндпоинты (смотреть в Swagger UI: http://localhost:8080/docs/)

### Аутентификация и пользователи

#### Публичные эндпоинты

- `POST /api/users` - регистрация нового пользователя
- `POST /api/auth/token` - авторизация пользователя (получение токенов)
- `POST /api/auth/token/refresh` - обновление JWT-токена по refresh-токену

#### Защищенные эндпоинты (требуют JWT-токен)

- `DELETE /api/auth/token` - выход из системы (инвалидация refresh-токенов пользователя)
- `GET /api/users/me` - получение информации о текущем пользователе
- `PATCH /api/users/me` - обновление данных пользователя (имя, фамилия)
- `PATCH /api/users/me/password` - изменение пароля пользователя
- `DELETE /api/users/me` - деактивация учетной записи текущего пользователя

### Банковские операции

#### Банковские счета (защищенные эндпоинты)

- `POST /api/accounts` - Создать новый банковский счет для аутентифицированного пользователя.
- `GET /api/accounts` - Получить список всех счетов пользователя.
- `GET /api/accounts/{accountId}` - Получить информацию о конкретном счете.
- `DELETE /api/accounts/{accountId}` - Закрыть банковский счет.

#### Транзакции (защищенные эндпоинты)

- `GET /api/accounts/{accountId}/transactions` - Получить историю транзакций по счету с возможностью фильтрации.
- `POST /api/accounts/{accountId}/transfer` - Перевести средства на другой счет по его ID.
- `POST /api/accounts/{accountId}/transfer-by-account` - Перевести средства на другой счет по его номеру счета.

## Запуск с помощью Docker

### Требования

- Docker
- Docker Compose

### Установка и запуск

Для запуска приложения используйте команду:

```bash
docker compose up -d
```

При первом запуске Docker автоматически:

1. Соберет образ приложения (используя multi-stage build)
2. Запустит PostgreSQL базу данных
3. Запустит банковское приложение после инициализации базы данных

### Доступ к API

После запуска сервисов API будет доступно по адресу:

- Swagger UI: http://localhost:8080/docs/

### Остановка приложения

Для остановки приложения используйте команду:

```bash
docker compose down
```

### Перезапуск с пересборкой после внесения изменений

Если вы внесли изменения в код, выполните:

```bash
docker compose up -d --build
```

## Особенности реализации

- **Функциональное программирование** - использование эффектов ZIO и функциональных паттернов на всех уровнях.
- **Типобезопасность**:
  - Использование **Scala 3** с его сильной системой типов.
  - Применение **уточненных типов (refined types)** с помощью библиотеки Iron для моделей данных (например, `Email`, `Password`, `UserId`), что обеспечивает валидацию на уровне типов.
  - Типобезопасные HTTP эндпоинты благодаря **Tapir**.
  - Типобезопасные SQL-запросы с **Quill**.
- **Улучшенная обработка ошибок**:
  - Четкое разделение бизнес-ошибок (например, `UserAlreadyExistsError`, `InvalidCredentialsError`) и ошибок валидации.
  - Стандартизированный формат `ErrorResponse` для API.
- **Миграции БД** - автоматические миграции схемы базы данных с помощью Flyway.
- **JWT-аутентификация** - полноценная система аутентификации с использованием access и refresh токенов.
- **Контейнеризация** - простое развертывание в Docker с использованием multi-stage сборки для оптимизации размера образа.
- **API-документация** - автоматическая генерация интерактивной Swagger UI документации из кода эндпоинтов.
- **Модульная структура** - четкое разделение ответственности между проектами и модулями.
- **Тестирование** - каждый функциональный модуль имеет свой набор тестов (модульные, интеграционные).
