-- Создание таблицы для хранения refresh токенов JWT
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token VARCHAR(1024) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Добавляем индексы для повышения производительности
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(refresh_token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);

-- Комментарий к таблице
COMMENT ON TABLE refresh_tokens IS 'Таблица для хранения refresh токенов JWT'; 