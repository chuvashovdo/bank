-- Создание базовых разрешений
INSERT INTO app_permissions (id, name, description, created_at, updated_at) VALUES
    (gen_random_uuid(), 'users:read', 'Просмотр пользователей', NOW(), NOW()),
    (gen_random_uuid(), 'users:create', 'Создание пользователей', NOW(), NOW()),
    (gen_random_uuid(), 'users:update', 'Обновление пользователей', NOW(), NOW()),
    (gen_random_uuid(), 'users:delete', 'Удаление пользователей', NOW(), NOW()),
    (gen_random_uuid(), 'accounts:read', 'Просмотр счетов', NOW(), NOW()),
    (gen_random_uuid(), 'accounts:create', 'Создание счетов', NOW(), NOW()),
    (gen_random_uuid(), 'accounts:update', 'Обновление счетов', NOW(), NOW()),
    (gen_random_uuid(), 'accounts:delete', 'Закрытие счетов', NOW(), NOW()),
    (gen_random_uuid(), 'transactions:read', 'Просмотр транзакций', NOW(), NOW()),
    (gen_random_uuid(), 'transactions:create', 'Создание транзакций', NOW(), NOW()),
    (gen_random_uuid(), 'admin:access', 'Доступ к админ-панели', NOW(), NOW()),
    (gen_random_uuid(), 'admin:accounts:read', 'Админский просмотр всех счетов', NOW(), NOW()),
    (gen_random_uuid(), 'admin:accounts:manage', 'Админское управление счетами', NOW(), NOW()),
    (gen_random_uuid(), 'admin:transactions:deposit', 'Админское пополнение счетов', NOW(), NOW()),
    (gen_random_uuid(), 'admin:transactions:withdraw', 'Админское снятие со счетов', NOW(), NOW()),
    (gen_random_uuid(), 'admin:transactions:read', 'Админский просмотр всех транзакций', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Создание базовых ролей
INSERT INTO app_roles (id, name, description, created_at, updated_at) VALUES
    (gen_random_uuid(), 'USER', 'Стандартная роль пользователя', NOW(), NOW()),
    (gen_random_uuid(), 'MANAGER', 'Роль менеджера для работы с пользователями', NOW(), NOW()),
    (gen_random_uuid(), 'ADMIN', 'Роль администратора с полным доступом', NOW(), NOW())
ON CONFLICT (name) DO NOTHING;

-- Связывание разрешений с ролями
-- USER роль
INSERT INTO app_role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM app_roles WHERE name = 'USER'),
    id
FROM app_permissions 
WHERE name IN ('users:read', 'accounts:read', 'accounts:create', 'transactions:read', 'transactions:create')
ON CONFLICT DO NOTHING;

-- MANAGER роль
INSERT INTO app_role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM app_roles WHERE name = 'MANAGER'),
    id
FROM app_permissions 
WHERE name IN ('users:read', 'users:create', 'users:update', 'accounts:read', 'accounts:create', 'accounts:update', 'transactions:read', 'transactions:create')
ON CONFLICT DO NOTHING;

-- ADMIN роль
INSERT INTO app_role_permissions (role_id, permission_id)
SELECT 
    (SELECT id FROM app_roles WHERE name = 'ADMIN'),
    id
FROM app_permissions 
WHERE name IN ('users:read', 'users:create', 'users:update', 'users:delete', 'accounts:read', 'accounts:create', 'accounts:update', 'accounts:delete', 'transactions:read', 'transactions:create', 'admin:access', 'admin:accounts:read', 'admin:accounts:manage', 'admin:transactions:deposit', 'admin:transactions:withdraw', 'admin:transactions:read')
ON CONFLICT DO NOTHING; 