CREATE TABLE IF NOT EXISTS app_refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    refresh_token VARCHAR(1024) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_refresh_tokens_token ON app_refresh_tokens(refresh_token);
CREATE INDEX idx_refresh_tokens_user_id ON app_refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_expires_at ON app_refresh_tokens(expires_at);
