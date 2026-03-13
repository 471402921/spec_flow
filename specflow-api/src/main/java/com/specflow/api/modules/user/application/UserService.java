package com.specflow.api.modules.user.application;

import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.domain.repository.UserRepository;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.api.util.LogMasker;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.regex.Pattern;

/**
 * User 应用服务
 *
 * <p>职责：
 * - 定义业务用例（Use Cases）
 * - 声明事务边界
 * - 编排 Domain 对象和 Repository
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    // 邮箱格式正则（简化版 RFC 5322）
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    // 密码格式正则：至少8位，包含字母和数字
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\W]{8,}$"
    );

    /**
     * 用户注册
     *
     * @param email    邮箱
     * @param password 密码
     * @param nickname 昵称（可为 null）
     * @return 创建的用户
     */
    public User register(String email, String password, String nickname) {
        log.info("Registering user with email: {}", email);

        // 校验邮箱格式
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("INVALID_EMAIL_FORMAT", "请输入有效的邮箱地址");
        }

        // 校验密码格式
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("INVALID_PASSWORD_FORMAT",
                    "密码至少8位，需包含字母和数字");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException("EMAIL_ALREADY_EXISTS", "该邮箱已被注册");
        }

        // 创建用户
        String passwordHash = passwordEncoder.encode(password);
        User user = User.create(email, passwordHash, nickname);
        userRepository.save(user);

        log.info("User registered successfully: id={}", user.getId());
        return user;
    }

    /**
     * 用户登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return Session token
     */
    public String login(String email, String password) {
        log.info("User login attempt: {}", email);

        // 查找用户
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new AuthenticationException("邮箱或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("邮箱或密码错误");
        }

        // 创建 Token
        String token = tokenProvider.createToken(user.getId());
        log.info("User logged in successfully: userId={}", user.getId());

        return token;
    }

    /**
     * 用户登出
     *
     * @param token Session token
     */
    public void logout(String token) {
        log.info("User logout: token={}", LogMasker.maskToken(token));
        tokenProvider.revokeToken(token);
    }

    /**
     * 根据 ID 获取用户
     *
     * @param userId 用户 ID
     * @return 用户
     */
    @Transactional(readOnly = true)
    public User getUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));
    }

    /**
     * 修改用户资料
     *
     * @param userId    用户 ID
     * @param nickname  新昵称（可为 null）
     * @param avatarUrl 新头像 URL（可为 null）
     * @return 更新后的用户
     */
    public User updateProfile(String userId, String nickname, String avatarUrl) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 校验昵称长度
        if (nickname != null && (nickname.length() < 2 || nickname.length() > 20)) {
            throw new BusinessException("NICKNAME_LENGTH_INVALID", "昵称长度需在2-20个字符之间");
        }

        user.updateProfile(nickname, avatarUrl);
        userRepository.save(user);

        log.info("Profile updated for user: {}", userId);
        return user;
    }

    /**
     * 修改密码
     *
     * @param userId      用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    public void changePassword(String userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("INCORRECT_PASSWORD", "当前密码错误");
        }

        // 校验新密码格式
        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            throw new BusinessException("INVALID_PASSWORD_FORMAT",
                    "密码至少8位，需包含字母和数字");
        }

        // 更新密码
        String newPasswordHash = passwordEncoder.encode(newPassword);
        user.changePassword(newPasswordHash);
        userRepository.save(user);

        log.info("Password changed for user: {}", userId);
    }
}
