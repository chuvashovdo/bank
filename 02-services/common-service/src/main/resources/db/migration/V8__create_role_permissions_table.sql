CREATE TABLE IF NOT EXISTS app_role_permissions (
    role_id UUID NOT NULL,
    permission_id UUID NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES app_roles(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES app_permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON app_role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission_id ON app_role_permissions(permission_id);

COMMENT ON TABLE app_role_permissions IS 'Связующая таблица между ролями и разрешениями'; 