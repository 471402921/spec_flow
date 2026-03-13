-- =============================================
-- SpecFlow Service - 初始数据库架构
-- Version: 1.0
-- Description: 创建 session 表及相关索引和触发器
-- =============================================

-- 创建 session 表
CREATE TABLE session (
    id            VARCHAR(36)   PRIMARY KEY,
    user_id       VARCHAR(36)   NOT NULL,
    token         VARCHAR(128)  NOT NULL UNIQUE,
    expired_at    TIMESTAMP     NOT NULL,
    revoked       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_session_token ON session(token);
CREATE INDEX idx_session_user_id ON session(user_id);
CREATE INDEX idx_session_expired_at ON session(expired_at);

-- 创建注释
COMMENT ON TABLE session IS '会话管理表';
COMMENT ON COLUMN session.id IS '会话 ID（UUID）';
COMMENT ON COLUMN session.user_id IS '用户 ID';
COMMENT ON COLUMN session.token IS '会话令牌（唯一）';
COMMENT ON COLUMN session.expired_at IS '过期时间';
COMMENT ON COLUMN session.revoked IS '是否已撤销';
COMMENT ON COLUMN session.created_at IS '创建时间';
COMMENT ON COLUMN session.updated_at IS '更新时间';

-- 创建触发器函数：自动更新 updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 session 表创建触发器
CREATE TRIGGER session_updated_at_trigger
BEFORE UPDATE ON session
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
