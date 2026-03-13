package com.specflow.api.modules.user.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * User 领域实体（纯 POJO，无框架依赖）
 *
 * <p>职责：
 * - 维护用户的业务规则
 * - 提供领域行为（软删除、修改资料等）
 * - 工厂方法（create）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    /**
     * 用户 ID（UUID）
     */
    private String id;

    /**
     * 邮箱（存储为小写）
     */
    private String email;

    /**
     * 密码哈希（bcrypt）
     */
    private String passwordHash;

    /**
     * 昵称（2-20字符）
     */
    private String nickname;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 软删除标记
     */
    private boolean deleted;

    /**
     * 删除时间
     */
    private Instant deletedAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 更新时间
     */
    private Instant updatedAt;

    // ==================== P2 新增字段 ====================

    /**
     * 邮箱是否已验证
     */
    private boolean emailVerified;

    /**
     * 连续失败登录次数
     */
    private int failedLoginAttempts;

    /**
     * 账号锁定截止时间（null 表示未锁定）
     */
    private Instant lockedUntil;

    // ==================== 领域行为 ====================

    /**
     * 修改用户资料
     *
     * @param nickname 新昵称
     * @param avatarUrl 新头像 URL
     */
    public void updateProfile(String nickname, String avatarUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (avatarUrl != null) {
            this.avatarUrl = avatarUrl;
        }
        this.updatedAt = Instant.now();
    }

    /**
     * 修改密码
     *
     * @param newPasswordHash 新密码哈希
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
        this.updatedAt = Instant.now();
    }

    /**
     * 软删除用户
     */
    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ==================== P2 新增领域行为 ====================

    /**
     * 验证邮箱
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.updatedAt = Instant.now();
    }

    /**
     * 检查账号是否被锁定
     *
     * @return true 如果账号已被锁定
     */
    public boolean isLocked() {
        if (this.lockedUntil == null) {
            return false;
        }
        return Instant.now().isBefore(this.lockedUntil);
    }

    /**
     * 获取剩余锁定时间（分钟）
     *
     * @return 剩余锁定分钟数，如果未锁定返回 0
     */
    public long getRemainingLockMinutes() {
        if (!isLocked()) {
            return 0;
        }
        long seconds = Duration.between(Instant.now(), this.lockedUntil).getSeconds();
        return Math.max(0, (seconds + 59) / 60); // 向上取整
    }

    /**
     * 记录登录失败
     *
     * @param maxAttempts 最大允许失败次数
     * @param lockMinutes 达到最大次数后锁定时长（分钟）
     */
    public void recordFailedLogin(int maxAttempts, int lockMinutes) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.lockedUntil = Instant.now().plusSeconds(lockMinutes * 60L);
        }
        this.updatedAt = Instant.now();
    }

    /**
     * 记录登录成功（重置失败计数和锁定）
     */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    /**
     * 锁定账号
     *
     * @param lockMinutes 锁定时长（分钟）
     */
    public void lockAccount(int lockMinutes) {
        this.lockedUntil = Instant.now().plusSeconds(lockMinutes * 60L);
        this.updatedAt = Instant.now();
    }

    /**
     * 解锁账号
     */
    public void unlockAccount() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = Instant.now();
    }

    // ==================== 工厂方法 ====================

    /**
     * 创建新用户
     *
     * @param email 邮箱（将被转为小写）
     * @param passwordHash 密码哈希
     * @param nickname 昵称（为 null 则使用邮箱@前缀）
     * @return User 实体
     */
    public static User create(String email, String passwordHash, String nickname) {
        Instant now = Instant.now();
        String normalizedEmail = email.toLowerCase();
        String finalNickname = nickname != null ? nickname : extractNicknameFromEmail(normalizedEmail);

        return new User(
                UUID.randomUUID().toString(),
                normalizedEmail,
                passwordHash,
                finalNickname,
                null,
                false,
                null,
                now,
                now,
                false,  // emailVerified
                0,      // failedLoginAttempts
                null    // lockedUntil
        );
    }

    /**
     * 从邮箱提取昵称（@前缀）
     */
    private static String extractNicknameFromEmail(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(0, atIndex) : email;
    }
}
