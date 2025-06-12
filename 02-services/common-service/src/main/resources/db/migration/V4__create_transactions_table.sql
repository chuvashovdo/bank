CREATE TABLE IF NOT EXISTS transactions (
    id UUID PRIMARY KEY,
    source_account_id UUID,
    destination_account_id UUID,
    amount DECIMAL(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    memo VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_source_account
        FOREIGN KEY(source_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL,

    CONSTRAINT fk_destination_account
        FOREIGN KEY(destination_account_id)
            REFERENCES accounts(id)
            ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_transactions_source_id ON transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_destination_id ON transactions(destination_account_id); 