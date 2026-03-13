-- =============================================
-- SoulPal Service - User Module P2 (Security Enhancement)
-- Version: 1.3
-- Description: 创建 verification_tokens 表，users 表增加安全相关字段
-- =============================================

-- users 表增加 P2 安全字段
ALTER TABLE users
    ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP;

-- 创建索引
CREATE INDEX idx_users_email_verified ON users(email_verified);
CREATE INDEX idx_users_locked_until ON users(locked_until);

-- 创建注释
COMMENT ON COLUMN users.email_verified IS '邮箱是否已验证';
COMMENT ON COLUMN users.failed_login_attempts IS '连续失败登录次数（用于账号锁定）';
COMMENT ON COLUMN users.locked_until IS '账号锁定截止时间（null 表示未锁定）';

-- 创建 verification_tokens 表
CREATE TABLE verification_tokens (
    id          VARCHAR(36)   PRIMARY KEY,
    token       VARCHAR(128)  NOT NULL UNIQUE,
    user_id     VARCHAR(36)   NOT NULL,
    type        VARCHAR(20)   NOT NULL,
    email       VARCHAR(255),
    used        BOOLEAN       NOT NULL DEFAULT FALSE,
    expired_at  TIMESTAMP     NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX idx_verification_tokens_user_type ON verification_tokens(user_id, type);
CREATE INDEX idx_verification_tokens_expired_at ON verification_tokens(expired_at);
CREATE INDEX idx_verification_tokens_used ON verification_tokens(used);

-- 创建外键约束
ALTER TABLE verification_tokens
    ADD CONSTRAINT fk_verification_tokens_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;

-- 创建注释
COMMENT ON TABLE verification_tokens IS '验证令牌表（邮箱验证、密码重置、邮箱修改）';
COMMENT ON COLUMN verification_tokens.id IS '令牌 ID（UUID）';
COMMENT ON COLUMN verification_tokens.token IS '令牌值（URL-safe，全局唯一）';
COMMENT ON COLUMN verification_tokens.user_id IS '关联用户 ID';
COMMENT ON COLUMN verification_tokens.type IS '令牌类型（EMAIL_VERIFICATION/PASSWORD_RESET/EMAIL_CHANGE）';
COMMENT ON COLUMN verification_tokens.email IS '目标邮箱（用于邮箱修改时存储新邮箱）';
COMMENT ON COLUMN verification_tokens.used IS '是否已使用';
COMMENT ON COLUMN verification_tokens.expired_at IS '过期时间';
COMMENT ON COLUMN verification_tokens.created_at IS '创建时间';
COMMENT ON COLUMN verification_tokens.updated_at IS '更新时间';

-- 为 verification_tokens 表创建触发器
CREATE TRIGGER verification_tokens_updated_at_trigger
BEFORE UPDATE ON verification_tokens
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
