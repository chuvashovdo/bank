CREATE TABLE IF NOT EXISTS app_user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES app_users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES app_roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_user_id ON app_user_roles(user_id);
CREATE INDEX idx_user_roles_role_id ON app_user_roles(role_id);

COMMENT ON TABLE app_user_roles IS 'Связующая таблица между пользователями и ролями'; 