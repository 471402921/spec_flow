-- =============================================
-- SpecFlow Service - User Module P0
-- Version: 1.1
-- Description: 创建 users 表和 pets 表
-- =============================================

-- 创建 users 表
CREATE TABLE users (
    id             VARCHAR(36)   PRIMARY KEY,
    email          VARCHAR(255)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    nickname       VARCHAR(20)   NOT NULL,
    avatar_url     VARCHAR(512),
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建邮箱唯一索引（小写邮箱）
CREATE UNIQUE INDEX idx_users_email ON users(LOWER(email)) WHERE deleted = FALSE;

-- 创建索引
CREATE INDEX idx_users_deleted ON users(deleted);
CREATE INDEX idx_users_deleted_at ON users(deleted_at);

-- 创建注释
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '用户 ID（UUID）';
COMMENT ON COLUMN users.email IS '邮箱（唯一，存储为小写）';
COMMENT ON COLUMN users.password_hash IS '密码哈希（bcrypt）';
COMMENT ON COLUMN users.nickname IS '昵称（2-20字符）';
COMMENT ON COLUMN users.avatar_url IS '头像 URL';
COMMENT ON COLUMN users.deleted IS '软删除标记';
COMMENT ON COLUMN users.deleted_at IS '删除时间（用于30天后物理清除）';
COMMENT ON COLUMN users.created_at IS '创建时间';
COMMENT ON COLUMN users.updated_at IS '更新时间';

-- 创建 pets 表
CREATE TABLE pets (
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

-- 创建索引
CREATE INDEX idx_pets_owner_id ON pets(owner_id);
CREATE INDEX idx_pets_owner_deleted ON pets(owner_id, deleted);
CREATE INDEX idx_pets_deleted ON pets(deleted);
CREATE INDEX idx_pets_deleted_at ON pets(deleted_at);

-- 创建外键约束
ALTER TABLE pets ADD CONSTRAINT fk_pets_owner
    FOREIGN KEY (owner_id) REFERENCES users(id);

-- 创建注释
COMMENT ON TABLE pets IS '宠物表';
COMMENT ON COLUMN pets.id IS '宠物 ID（UUID）';
COMMENT ON COLUMN pets.owner_id IS '主人 ID（外键关联 users）';
COMMENT ON COLUMN pets.name IS '宠物名字（1-30字符）';
COMMENT ON COLUMN pets.species IS '种类（DOG/CAT）';
COMMENT ON COLUMN pets.breed IS '品种（1-50字符）';
COMMENT ON COLUMN pets.gender IS '性别（MALE/FEMALE）';
COMMENT ON COLUMN pets.birthday IS '生日';
COMMENT ON COLUMN pets.avatar_url IS '头像 URL';
COMMENT ON COLUMN pets.deleted IS '软删除标记';
COMMENT ON COLUMN pets.deleted_at IS '删除时间';
COMMENT ON COLUMN pets.created_at IS '创建时间';
COMMENT ON COLUMN pets.updated_at IS '更新时间';

-- 为 users 表创建触发器
CREATE TRIGGER users_updated_at_trigger
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 为 pets 表创建触发器
CREATE TRIGGER pets_updated_at_trigger
BEFORE UPDATE ON pets
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
