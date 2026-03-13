package com.specflow.api.modules.auth.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflow.api.modules.auth.application.SessionService;
import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.interfaces.dto.CreateSessionRequest;
import com.specflow.common.exception.AuthenticationException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SessionController 集成测试
 *
 * 测试策略：
 * - 使用 @SpringBootTest + @AutoConfigureMockMvc 进行 Web 层测试
 * - Mock SessionService 层
 * - 验证 HTTP 请求/响应格式
 * - 验证异常处理
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("SessionController 集成测试")
class SessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SessionService sessionService;

    private String testUserId;
    private String testToken;
    private Session mockSession;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        testToken = "token_abc123def456";
        mockSession = Session.create(testUserId, testToken, 30);
    }

    @Test
    @DisplayName("POST /api/v1/sessions - 创建会话成功")
    void createSession_shouldReturn200() throws Exception {
        // Given
        CreateSessionRequest request = new CreateSessionRequest();
        request.setUserId(testUserId);

        when(sessionService.createSession(testUserId)).thenReturn(mockSession);

        // When & Then
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(testUserId)))
                .andExpect(jsonPath("$.data.token", is(testToken)))
                .andExpect(jsonPath("$.data.revoked", is(false)))
                .andExpect(jsonPath("$.data.expiredAt", notNullValue()));

        verify(sessionService).createSession(testUserId);
    }

    @Test
    @DisplayName("POST /api/v1/sessions - 缺少 userId 参数应返回 400")
    void createSession_withoutUserId_shouldReturn400() throws Exception {
        // Given
        CreateSessionRequest request = new CreateSessionRequest();
        // userId 为 null

        // When & Then
        mockMvc.perform(post("/api/v1/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{token} - 查询会话成功")
    void getSession_shouldReturn200() throws Exception {
        // Given
        when(sessionService.getSessionByToken(testToken)).thenReturn(mockSession);

        // When & Then
        mockMvc.perform(get("/api/v1/sessions/{token}", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(testUserId)))
                .andExpect(jsonPath("$.data.token", is(testToken)));

        verify(sessionService).getSessionByToken(testToken);
    }

    @Test
    @DisplayName("GET /api/v1/sessions/{token} - 会话不存在应返回 404")
    void getSession_whenNotFound_shouldReturn404() throws Exception {
        // Given
        when(sessionService.getSessionByToken(anyString()))
                .thenThrow(new NotFoundException("会话不存在"));

        // When & Then
        mockMvc.perform(get("/api/v1/sessions/{token}", "non-existent-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("会话不存在")));
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{token}/validate - 验证有效会话成功")
    void validateSession_whenValid_shouldReturn200() throws Exception {
        // Given
        when(sessionService.validateSession(testToken)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/v1/sessions/{token}/validate", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", is(true)));

        verify(sessionService).validateSession(testToken);
    }

    @Test
    @DisplayName("POST /api/v1/sessions/{token}/validate - 验证无效会话应返回 401")
    void validateSession_whenInvalid_shouldReturn401() throws Exception {
        // Given
        when(sessionService.validateSession(anyString()))
                .thenThrow(new AuthenticationException("会话已过期"));

        // When & Then
        mockMvc.perform(post("/api/v1/sessions/{token}/validate", testToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTHENTICATION_FAILED")))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("会话已过期")));
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{token} - 撤销会话成功")
    void revokeSession_shouldReturn200() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/sessions/{token}", testToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(sessionService).revokeSession(testToken);
    }

    @Test
    @DisplayName("DELETE /api/v1/sessions/{token} - 撤销不存在的会话应返回 404")
    void revokeSession_whenNotFound_shouldReturn404() throws Exception {
        // Given
        doThrow(new NotFoundException("会话不存在"))
                .when(sessionService).revokeSession(anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/sessions/{token}", "non-existent-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("NOT_FOUND")))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("会话不存在")));
    }
}
