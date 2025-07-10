CREATE TABLE IF NOT EXISTS app_permissions (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_permissions_name ON app_permissions(LOWER(name));

COMMENT ON TABLE app_permissions IS 'Хранит информацию о разрешениях (например, accounts:read, users:create)';
COMMENT ON COLUMN app_permissions.name IS 'Уникальное имя разрешения';
COMMENT ON COLUMN app_permissions.description IS 'Описание разрешения'; 