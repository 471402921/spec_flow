package com.specflow.api.modules.user.infrastructure.email;

/**
 * 邮件服务接口
 *
 * <p>职责：
 * - 定义邮件发送的契约
 * - P2 先用日志模拟实现，后续可替换为真实邮件服务（SMTP、SendGrid、AWS SES 等）
 *
 * <p>实现类：
 * - {@link LogEmailService} - 日志模拟实现（开发/测试环境）
 */
public interface EmailService {

    /**
     * 发送邮箱验证邮件
     *
     * @param toEmail 目标邮箱
     * @param token   验证令牌
     */
    void sendVerificationEmail(String toEmail, String token);

    /**
     * 发送密码重置邮件
     *
     * @param toEmail 目标邮箱
     * @param token   重置令牌
     */
    void sendPasswordResetEmail(String toEmail, String token);

    /**
     * 发送邮箱修改验证邮件
     *
     * @param toEmail 目标邮箱（新邮箱）
     * @param token   验证令牌
     */
    void sendEmailChangeVerification(String toEmail, String token);
}
