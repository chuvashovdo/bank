CREATE TABLE IF NOT EXISTS app_roles (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_roles_name ON app_roles(LOWER(name));

COMMENT ON TABLE app_roles IS 'Хранит информацию о ролях (например, АДМИН, ПОЛЬЗОВАТЕЛЬ)';
COMMENT ON COLUMN app_roles.name IS 'Уникальное имя роли';
COMMENT ON COLUMN app_roles.description IS 'Описание роли'; 