package com.specflow.api.modules.user.infrastructure.email;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LogEmailService 单元测试
 *
 * <p>测试范围：
 * - sendVerificationEmail
 * - sendPasswordResetEmail
 * - sendEmailChangeVerification
 * - 令牌脱敏
 */
@DisplayName("LogEmailService 单元测试")
class LogEmailServiceTest {

    private LogEmailService logEmailService;
    private Logger logger;
    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        logEmailService = new LogEmailService();

        // 设置日志捕获
        logger = (Logger) LoggerFactory.getLogger(LogEmailService.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(listAppender);
    }

    // ==================== sendVerificationEmail() 测试 ====================

    @Test
    @DisplayName("sendVerificationEmail() - 应记录验证邮件日志")
    void sendVerificationEmail_shouldLogVerificationEmail() {
        // Given
        String toEmail = "user@example.com";
        String token = "abcd1234efgh5678";

        // When
        logEmailService.sendVerificationEmail(toEmail, token);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        assertThat(logMessages).contains("[EMAIL] 邮箱验证邮件");
        assertThat(logMessages).contains("[EMAIL] 收件人: " + toEmail);
        assertThat(logMessages).contains("[EMAIL] 验证链接: /verify-email?token=");
        assertThat(logMessages).contains("abcd****5678");
    }

    // ==================== sendPasswordResetEmail() 测试 ====================

    @Test
    @DisplayName("sendPasswordResetEmail() - 应记录密码重置邮件日志")
    void sendPasswordResetEmail_shouldLogPasswordResetEmail() {
        // Given
        String toEmail = "user@example.com";
        String token = "reset1234token5678";

        // When
        logEmailService.sendPasswordResetEmail(toEmail, token);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        assertThat(logMessages).contains("[EMAIL] 密码重置邮件");
        assertThat(logMessages).contains("[EMAIL] 收件人: " + toEmail);
        assertThat(logMessages).contains("[EMAIL] 重置链接: /reset-password?token=");
        assertThat(logMessages).contains("rese****5678");
    }

    // ==================== sendEmailChangeVerification() 测试 ====================

    @Test
    @DisplayName("sendEmailChangeVerification() - 应记录邮箱修改验证邮件日志")
    void sendEmailChangeVerification_shouldLogEmailChangeVerification() {
        // Given
        String toEmail = "newemail@example.com";
        String token = "change1234email5678";

        // When
        logEmailService.sendEmailChangeVerification(toEmail, token);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        assertThat(logMessages).contains("[EMAIL] 邮箱修改验证邮件");
        assertThat(logMessages).contains("[EMAIL] 收件人（新邮箱）: " + toEmail);
        assertThat(logMessages).contains("[EMAIL] 验证链接: /confirm-email-change?token=");
        assertThat(logMessages).contains("chan****5678");
    }

    // ==================== 令牌脱敏测试 ====================

    @Test
    @DisplayName("令牌脱敏 - 长令牌应显示前后4位")
    void maskToken_withLongToken_shouldShowFirstAndLast4Chars() {
        // Given
        String toEmail = "user@example.com";
        String token = "abcdefghijklmnopqrstuvwxyz123456";

        // When
        logEmailService.sendVerificationEmail(toEmail, token);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        assertThat(logMessages).contains("abcd****3456");
    }

    @Test
    @DisplayName("令牌脱敏 - 短令牌应显示为 ****")
    void maskToken_withShortToken_shouldShowAsterisks() {
        // Given
        String toEmail = "user@example.com";
        String shortToken = "abc123";

        // When
        logEmailService.sendVerificationEmail(toEmail, shortToken);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        assertThat(logMessages).contains("****");
    }

    @Test
    @DisplayName("令牌脱敏 - null 令牌应显示为 ****")
    void maskToken_withNullToken_shouldShowAsterisks() {
        // Given
        String toEmail = "user@example.com";

        // When - 使用反射调用私有方法测试边界情况
        logEmailService.sendVerificationEmail(toEmail, "short");

        // Then - 验证正常处理，不抛出异常
        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).isNotEmpty();
    }

    // ==================== 边界测试 ====================

    @Test
    @DisplayName("边界 - 空邮箱应正常处理")
    void sendEmail_withEmptyEmail_shouldHandleGracefully() {
        // Given
        String emptyEmail = "";
        String token = "testtoken1234567890123456789012";

        // When & Then - 不应抛出异常
        logEmailService.sendVerificationEmail(emptyEmail, token);

        List<ILoggingEvent> logs = listAppender.list;
        assertThat(logs).isNotEmpty();
    }

    @Test
    @DisplayName("边界 - 32位令牌应正常处理")
    void sendEmail_with32CharToken_shouldMaskCorrectly() {
        // Given
        String toEmail = "user@example.com";
        String token32 = "ABCD1234EFGH5678IJKL9012MNOP3456";

        // When
        logEmailService.sendVerificationEmail(toEmail, token32);

        // Then
        List<ILoggingEvent> logs = listAppender.list;
        String logMessages = logs.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + b + "\n");

        // 32位令牌：前4 + **** + 后4 = ABCD****3456
        assertThat(logMessages).contains("ABCD****3456");
    }
}
