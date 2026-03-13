package com.specflow.api.modules.user.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflow.api.modules.user.application.UserService;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.interfaces.dto.ChangePasswordRequest;
import com.specflow.api.modules.user.interfaces.dto.LoginRequest;
import com.specflow.api.modules.user.interfaces.dto.RegisterRequest;
import com.specflow.api.modules.user.interfaces.dto.UpdateProfileRequest;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 集成测试
 *
 * <p>测试策略：
 * - 使用 @SpringBootTest + @AutoConfigureMockMvc 测试 Web 层
 * - Mock UserService 和 SessionService
 * - 验证 HTTP 请求/响应、参数校验、错误码
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController 集成测试")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private TokenProvider tokenProvider;

    private static final String TEST_USER_ID = "user-123";
    private static final String TEST_TOKEN = "token_abc123xyz";

    @BeforeEach
    void setUp() {
        // 默认设置：模拟有效的 token
        when(tokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        when(tokenProvider.getUserIdByToken(TEST_TOKEN)).thenReturn(TEST_USER_ID);
        when(tokenProvider.getExpiredAtByToken(TEST_TOKEN))
                .thenReturn(java.time.Instant.now().plusSeconds(30L * 24 * 60 * 60));
    }

    // ==================== register() 测试 ====================

    @Test
    @DisplayName("POST /api/v1/users/register - 成功注册")
    void register_withValidData_shouldReturn201() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123");
        request.setNickname("TestUser");

        User mockUser = createMockUser(TEST_USER_ID, "test@example.com", "TestUser");
        when(userService.register(anyString(), anyString(), anyString())).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_USER_ID)))
                .andExpect(jsonPath("$.data.email", is("test@example.com")))
                .andExpect(jsonPath("$.data.nickname", is("TestUser")));
    }

    @Test
    @DisplayName("POST /api/v1/users/register - 邮箱格式无效")
    void register_withInvalidEmail_shouldReturn400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("invalid-email");
        request.setPassword("Password123");

        // When & Then - 参数校验失败，返回 VALIDATION_FAILED
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/users/register - 邮箱已存在")
    void register_withExistingEmail_shouldReturn409() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");
        request.setPassword("Password123");

        when(userService.register(anyString(), anyString(), any()))
                .thenThrow(new BusinessException("EMAIL_ALREADY_EXISTS", "该邮箱已被注册"));

        // When & Then
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("EMAIL_ALREADY_EXISTS")));
    }

    @Test
    @DisplayName("POST /api/v1/users/register - 密码少于8位")
    void register_withShortPassword_shouldReturn400() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("short1"); // 少于8位

        // When & Then - 参数校验失败
        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== login() 测试 ====================

    @Test
    @DisplayName("POST /api/v1/users/login - 成功登录")
    void login_withValidCredentials_shouldReturn200() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("Password123");

        when(userService.login(anyString(), anyString())).thenReturn(TEST_TOKEN);

        User mockUser = createMockUser(TEST_USER_ID, "test@example.com", "TestUser");
        when(userService.getUserById(TEST_USER_ID)).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", is(TEST_TOKEN)))
                .andExpect(jsonPath("$.data.user.id", is(TEST_USER_ID)))
                .andExpect(jsonPath("$.data.user.email", is("test@example.com")));
    }

    @Test
    @DisplayName("POST /api/v1/users/login - 邮箱或密码错误")
    void login_withWrongCredentials_shouldReturn401() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("WrongPassword");

        when(userService.login(anyString(), anyString()))
                .thenThrow(new AuthenticationException("邮箱或密码错误"));

        // When & Then
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")));
    }

    @Test
    @DisplayName("POST /api/v1/users/login - 邮箱为空")
    void login_withEmptyEmail_shouldReturn400() throws Exception {
        // Given
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("Password123");

        // When & Then
        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==================== logout() 测试 ====================

    @Test
    @DisplayName("POST /api/v1/users/logout - 成功登出")
    void logout_withValidToken_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/logout")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/v1/users/logout - 无 Token 应返回 401")
    void logout_withoutToken_shouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/users/logout"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== getCurrentUser() 测试 ====================

    @Test
    @DisplayName("GET /api/v1/users/me - 获取当前用户信息")
    void getCurrentUser_withValidToken_shouldReturn200() throws Exception {
        // Given
        User mockUser = createMockUser(TEST_USER_ID, "test@example.com", "TestUser");
        when(userService.getUserById(TEST_USER_ID)).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + TEST_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(TEST_USER_ID)))
                .andExpect(jsonPath("$.data.email", is("test@example.com")))
                .andExpect(jsonPath("$.data.nickname", is("TestUser")))
                .andExpect(jsonPath("$.data.createdAt", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - 无 Token 应返回 401")
    void getCurrentUser_withoutToken_shouldReturn401() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== updateProfile() 测试 ====================

    @Test
    @DisplayName("PUT /api/v1/users/me - 成功更新资料")
    void updateProfile_withValidData_shouldReturn200() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewNickname");
        request.setAvatarUrl("https://example.com/new-avatar.jpg");

        User updatedUser = createMockUser(TEST_USER_ID, "test@example.com", "NewNickname");
        when(userService.updateProfile(anyString(), anyString(), anyString())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.nickname", is("NewNickname")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 昵称过短应返回 400")
    void updateProfile_withTooShortNickname_shouldReturn400() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("A"); // 少于2字符

        // When & Then - 参数校验失败，返回 VALIDATION_FAILED
        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me - 无 Token 应返回 401")
    void updateProfile_withoutToken_shouldReturn401() throws Exception {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("NewName");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== changePassword() 测试 ====================

    @Test
    @DisplayName("PUT /api/v1/users/me/password - 成功修改密码")
    void changePassword_withCorrectOldPassword_shouldReturn200() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("NewPass456");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password - 旧密码错误")
    void changePassword_withWrongOldPassword_shouldReturn400() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("WrongOldPass");
        request.setNewPassword("NewPass456");

        doThrow(new BusinessException("INCORRECT_PASSWORD", "当前密码错误"))
                .when(userService).changePassword(anyString(), anyString(), anyString());

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("INCORRECT_PASSWORD")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password - 新密码少于8位")
    void changePassword_withShortNewPassword_shouldReturn400() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("short1"); // 少于8位

        // When & Then - 参数校验失败，返回 VALIDATION_FAILED
        mockMvc.perform(put("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + TEST_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("PUT /api/v1/users/me/password - 无 Token 应返回 401")
    void changePassword_withoutToken_shouldReturn401() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("OldPass123");
        request.setNewPassword("NewPass456");

        // When & Then
        mockMvc.perform(put("/api/v1/users/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 辅助方法 ====================

    private User createMockUser(String id, String email, String nickname) {
        User user = User.create(email, "hashedPassword", nickname);
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
