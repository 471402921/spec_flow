package com.specflow.api.modules.user.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志模拟邮件服务实现
 *
 * <p>职责：
 * - 将邮件内容打印到日志，不实际发送邮件
 * - 用于开发环境和 P2 初期测试
 * - 后续可替换为真实邮件服务实现
 */
@Slf4j
@Service
public class LogEmailService implements EmailService {

    private static final String LOG_SEPARATOR = "\n" + "=".repeat(60);

    @Override
    public void sendVerificationEmail(String toEmail, String token) {
        log.info(LOG_SEPARATOR);
        log.info("[EMAIL] 邮箱验证邮件");
        log.info("[EMAIL] 收件人: {}", toEmail);
        log.info("[EMAIL] 验证令牌: {}", maskToken(token));
        log.info("[EMAIL] 验证链接: /verify-email?token={}", maskToken(token));
        log.info(LOG_SEPARATOR);
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String token) {
        log.info(LOG_SEPARATOR);
        log.info("[EMAIL] 密码重置邮件");
        log.info("[EMAIL] 收件人: {}", toEmail);
        log.info("[EMAIL] 重置令牌: {}", maskToken(token));
        log.info("[EMAIL] 重置链接: /reset-password?token={}", maskToken(token));
        log.info(LOG_SEPARATOR);
    }

    @Override
    public void sendEmailChangeVerification(String toEmail, String token) {
        log.info(LOG_SEPARATOR);
        log.info("[EMAIL] 邮箱修改验证邮件");
        log.info("[EMAIL] 收件人（新邮箱）: {}", toEmail);
        log.info("[EMAIL] 验证令牌: {}", maskToken(token));
        log.info("[EMAIL] 验证链接: /confirm-email-change?token={}", maskToken(token));
        log.info(LOG_SEPARATOR);
    }

    /**
     * 令牌脱敏显示
     *
     * @param token 原始令牌
     * @return 脱敏后的令牌
     */
    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "****";
        }
        return token.substring(0, 4) + "****" + token.substring(token.length() - 4);
    }
}
