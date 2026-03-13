-- =============================================
-- SoulPal Service - Family Module P1
-- Version: 1.2
-- Description: 创建家庭相关表（families, family_members, family_invitations）
-- =============================================

-- 创建 families 表
CREATE TABLE families (
    id          VARCHAR(36)   PRIMARY KEY,
    name        VARCHAR(20)   NOT NULL,
    owner_id    VARCHAR(36)   NOT NULL,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_families_owner_id ON families(owner_id);

-- 创建注释
COMMENT ON TABLE families IS '家庭表（解散时物理删除）';
COMMENT ON COLUMN families.id IS '家庭 ID（UUID）';
COMMENT ON COLUMN families.name IS '家庭名称（2-20字符）';
COMMENT ON COLUMN families.owner_id IS '当前主人 ID（冗余字段）';
COMMENT ON COLUMN families.created_at IS '创建时间';
COMMENT ON COLUMN families.updated_at IS '更新时间';

-- 创建 family_members 表
CREATE TABLE family_members (
    id          VARCHAR(36)   PRIMARY KEY,
    family_id   VARCHAR(36)   NOT NULL,
    user_id     VARCHAR(36)   NOT NULL,
    role        VARCHAR(10)   NOT NULL,
    joined_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建唯一索引（同一用户在同一家庭只有一条记录）
CREATE UNIQUE INDEX idx_family_members_family_user ON family_members(family_id, user_id);
CREATE INDEX idx_family_members_family_id ON family_members(family_id);
CREATE INDEX idx_family_members_user_id ON family_members(user_id);
CREATE INDEX idx_family_members_role ON family_members(role);

-- 创建外键约束
ALTER TABLE family_members ADD CONSTRAINT fk_family_members_family
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE;

-- 创建注释
COMMENT ON TABLE family_members IS '家庭成员关系表';
COMMENT ON COLUMN family_members.id IS '关系 ID（UUID）';
COMMENT ON COLUMN family_members.family_id IS '家庭 ID';
COMMENT ON COLUMN family_members.user_id IS '用户 ID';
COMMENT ON COLUMN family_members.role IS '角色（OWNER/MEMBER）';
COMMENT ON COLUMN family_members.joined_at IS '加入时间';
COMMENT ON COLUMN family_members.created_at IS '创建时间';
COMMENT ON COLUMN family_members.updated_at IS '更新时间';

-- 创建 family_invitations 表
CREATE TABLE family_invitations (
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
CREATE INDEX idx_family_invitations_family_id ON family_invitations(family_id);
CREATE INDEX idx_family_invitations_code ON family_invitations(code);
CREATE INDEX idx_family_invitations_expired_at ON family_invitations(expired_at);

-- 创建外键约束
ALTER TABLE family_invitations ADD CONSTRAINT fk_family_invitations_family
    FOREIGN KEY (family_id) REFERENCES families(id) ON DELETE CASCADE;

-- 创建注释
COMMENT ON TABLE family_invitations IS '家庭邀请码表';
COMMENT ON COLUMN family_invitations.id IS '邀请码 ID（UUID）';
COMMENT ON COLUMN family_invitations.family_id IS '家庭 ID';
COMMENT ON COLUMN family_invitations.code IS '邀请码（8位，全局唯一）';
COMMENT ON COLUMN family_invitations.created_by IS '创建者 ID（家庭主人）';
COMMENT ON COLUMN family_invitations.revoked IS '是否已撤销';
COMMENT ON COLUMN family_invitations.expired_at IS '过期时间';
COMMENT ON COLUMN family_invitations.created_at IS '创建时间';
COMMENT ON COLUMN family_invitations.updated_at IS '更新时间';

-- 为 families 表创建触发器
CREATE TRIGGER families_updated_at_trigger
BEFORE UPDATE ON families
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 为 family_members 表创建触发器
CREATE TRIGGER family_members_updated_at_trigger
BEFORE UPDATE ON family_members
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- 为 family_invitations 表创建触发器
CREATE TRIGGER family_invitations_updated_at_trigger
BEFORE UPDATE ON family_invitations
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
