-- =============================================
-- SpecFlow Service - H2 测试数据库架构
-- Description: H2 兼容的简化 schema（不包含触发器和函数）
-- =============================================

-- 创建 session 表
CREATE TABLE IF NOT EXISTS session (
    id            VARCHAR(36)   PRIMARY KEY,
    user_id       VARCHAR(36)   NOT NULL,
    token         VARCHAR(128)  NOT NULL UNIQUE,
    expired_at    TIMESTAMP     NOT NULL,
    revoked       BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_session_token ON session(token);
CREATE INDEX IF NOT EXISTS idx_session_user_id ON session(user_id);
CREATE INDEX IF NOT EXISTS idx_session_expired_at ON session(expired_at);

-- 创建 users 表
CREATE TABLE IF NOT EXISTS users (
    id             VARCHAR(36)   PRIMARY KEY,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    nickname       VARCHAR(20)   NOT NULL,
    avatar_url     VARCHAR(512),
    deleted               BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at            TIMESTAMP,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    email_verified        BOOLEAN       NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER       NOT NULL DEFAULT 0,
    locked_until          TIMESTAMP
);

-- 创建 users 表索引（H2 语法）
-- 注意：H2 不支持部分索引 (WHERE deleted = FALSE) 和函数索引 (LOWER(email))
-- 生产环境使用：CREATE UNIQUE INDEX idx_users_email ON users(LOWER(email)) WHERE deleted = FALSE;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_deleted ON users(deleted);
CREATE INDEX IF NOT EXISTS idx_users_deleted_at ON users(deleted_at);
CREATE INDEX IF NOT EXISTS idx_users_email_verified ON users(email_verified);
CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users(locked_until);

-- 创建 pets 表
CREATE TABLE IF NOT EXISTS pets (
    id          VARCHAR(36)   PRIMARY KEY,
    owner_id    VARCHAR(36)   NOT NULL,
    name        VARCHAR(30)   NOT NULL,
    species     VARCHAR(10)   NOT NULL,
    breed       VARCHAR(50)   NOT NULL,
    gender      VARCHAR(10)   NOT NULL,
    birthday    DATE,
    avatar_url  VARCHAR(512),
    deleted     BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建 pets 表索引
CREATE INDEX IF NOT EXISTS idx_pets_owner_id ON pets(owner_id);
CREATE INDEX IF NOT EXISTS idx_pets_owner_deleted ON pets(owner_id, deleted);
CREATE INDEX IF NOT EXISTS idx_pets_deleted ON pets(deleted);
CREATE INDEX IF NOT EXISTS idx_pets_deleted_at ON pets(deleted_at);

-- =============================================
-- Family Module P1
-- =============================================

-- 创建 families 表
CREATE TABLE IF NOT EXISTS families (
    id          VARCHAR(36)   PRIMARY KEY,
    name        VARCHAR(20)   NOT NULL,
    owner_id    VARCHAR(36)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_families_owner_id ON families(owner_id);

-- 创建 family_members 表
CREATE TABLE IF NOT EXISTS family_members (
    id          VARCHAR(36)   PRIMARY KEY,
    family_id   VARCHAR(36)   NOT NULL,
    user_id     VARCHAR(36)   NOT NULL,
    role        VARCHAR(10)   NOT NULL,
    joined_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE UNIQUE INDEX IF NOT EXISTS idx_family_members_family_user ON family_members(family_id, user_id);
CREATE INDEX IF NOT EXISTS idx_family_members_family_id ON family_members(family_id);
CREATE INDEX IF NOT EXISTS idx_family_members_user_id ON family_members(user_id);
CREATE INDEX IF NOT EXISTS idx_family_members_role ON family_members(role);

-- 创建 family_invitations 表
CREATE TABLE IF NOT EXISTS family_invitations (
    id          VARCHAR(36)   PRIMARY KEY,
    family_id   VARCHAR(36)   NOT NULL,
    code        VARCHAR(8)    NOT NULL UNIQUE,
    created_by  VARCHAR(36)   NOT NULL,
    revoked     BOOLEAN       NOT NULL DEFAULT FALSE,
    expired_at  TIMESTAMP     NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_family_invitations_family_id ON family_invitations(family_id);
CREATE INDEX IF NOT EXISTS idx_family_invitations_code ON family_invitations(code);
CREATE INDEX IF NOT EXISTS idx_family_invitations_expired_at ON family_invitations(expired_at);

-- =============================================
-- User Module P2 (Security Enhancement)
-- =============================================

-- 创建 verification_tokens 表
CREATE TABLE IF NOT EXISTS verification_tokens (
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
CREATE INDEX IF NOT EXISTS idx_verification_tokens_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_id ON verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_user_type ON verification_tokens(user_id, type);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_expired_at ON verification_tokens(expired_at);
CREATE INDEX IF NOT EXISTS idx_verification_tokens_used ON verification_tokens(used);
