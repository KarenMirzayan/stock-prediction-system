ALTER TABLE users
    ADD COLUMN IF NOT EXISTS telegram_chat_id         BIGINT,
    ADD COLUMN IF NOT EXISTS telegram_username        VARCHAR(100),
    ADD COLUMN IF NOT EXISTS telegram_link_token      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS telegram_link_token_expiry TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_user_telegram_link_token ON users (telegram_link_token)
    WHERE telegram_link_token IS NOT NULL;
