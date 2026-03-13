# 集成测试规则（Interfaces 层 Controller）

## Controller 集成测试模板

```java
package com.specflow.api.modules.{module}.interfaces;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.specflow.api.modules.{module}.application.{Service};
import com.specflow.api.modules.{module}.domain.entity.{Entity};
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({Controller}.class)
@DisplayName("{Controller} 集成测试")
class {Controller}Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private {Service} {service};

    // --- POST 创建 ---

    @Test
    @DisplayName("POST /api/v1/{resource} - 成功创建")
    void create{Resource}_shouldReturnSuccess() throws Exception {
        // Given
        {Entity} mock{Entity} = {Entity}.create(/* params */);
        when({service}.create{Resource}(anyString())).thenReturn(mock{Entity});

        // When & Then
        mockMvc.perform(post("/api/v1/{resource}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"field\":\"value\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/{resource} - 请求参数无效")
    void create{Resource}_withInvalidRequest_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/{resource}")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // --- GET 查询 ---

    @Test
    @DisplayName("GET /api/v1/{resource}/{id} - 存在时返回")
    void get{Resource}_whenExists_shouldReturnResource() throws Exception {
        // Given
        {Entity} mock{Entity} = {Entity}.create(/* params */);
        when({service}.get{Resource}ById(anyString())).thenReturn(mock{Entity});

        // When & Then
        mockMvc.perform(get("/api/v1/{resource}/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").isNotEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/{resource}/{id} - 不存在时 404")
    void get{Resource}_whenNotExists_shouldReturnNotFound() throws Exception {
        // Given
        when({service}.get{Resource}ById(anyString()))
                .thenThrow(new NotFoundException("资源不存在"));

        // When & Then
        mockMvc.perform(get("/api/v1/{resource}/non-existent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // --- DELETE 删除/撤销 ---

    @Test
    @DisplayName("DELETE /api/v1/{resource}/{id} - 成功删除")
    void delete{Resource}_shouldReturnSuccess() throws Exception {
        mockMvc.perform(delete("/api/v1/{resource}/test-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("DELETE /api/v1/{resource}/{id} - 不存在时 404")
    void delete{Resource}_whenNotExists_shouldReturnNotFound() throws Exception {
        // Given - mock void 方法抛异常
        org.mockito.Mockito.doThrow(new NotFoundException("资源不存在"))
                .when({service}).delete{Resource}(anyString());

        // When & Then
        mockMvc.perform(delete("/api/v1/{resource}/non-existent"))
                .andExpect(status().isNotFound());
    }
}
```

## 核心规则

### TEST-I001: 使用 @WebMvcTest 而非 @SpringBootTest

```java
// ✅ 正确：只加载指定 Controller，速度快
@WebMvcTest(SessionController.class)
class SessionControllerTest { }

// ❌ 错误：加载完整 Spring 上下文，太慢
@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerTest { }
```

### TEST-I002: @MockBean 声明服务依赖

```java
// ✅ 正确：Mock 掉 Application 层服务
@MockBean
private SessionService sessionService;

// ❌ 错误：直接注入真实服务
@Autowired
private SessionService sessionService;  // @WebMvcTest 不加载 Service
```

### TEST-I003: 验证 HTTP 状态码 + 响应体

```java
// ✅ 正确：同时验证状态码和响应体结构
mockMvc.perform(get("/api/v1/sessions/test-token"))
        .andExpect(status().isOk())                      // HTTP 200
        .andExpect(jsonPath("$.code").value(200))         // 业务状态码
        .andExpect(jsonPath("$.data.token").isNotEmpty()) // 数据字段
        .andExpect(jsonPath("$.data.userId").value("test-user"));

// ❌ 错误：只验证状态码
mockMvc.perform(get("/api/v1/sessions/test-token"))
        .andExpect(status().isOk());  // 不知道响应体是否正确
```

### TEST-I004: 异常场景验证统一错误响应

项目使用 GlobalExceptionHandler 统一处理异常，测试需验证错误响应格式：

```java
// NotFoundException → 404
mockMvc.perform(get("/api/v1/sessions/bad"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(404))
        .andExpect(jsonPath("$.message").isNotEmpty());

// AuthenticationException → 401
mockMvc.perform(post("/api/v1/sessions/bad/validate"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value(401))
        .andExpect(jsonPath("$.message").isNotEmpty());

// MethodArgumentNotValidException → 400
mockMvc.perform(post("/api/v1/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest());
```

### TEST-I005: POST 请求构造 JSON Body

```java
// ✅ 方式 1：直接写 JSON 字符串（简单场景）
mockMvc.perform(post("/api/v1/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"userId\":\"test-user\"}"));

// ✅ 方式 2：使用 ObjectMapper（复杂对象）
CreateSessionRequest request = new CreateSessionRequest();
request.setUserId("test-user");

mockMvc.perform(post("/api/v1/sessions")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request)));
```

### TEST-I006: void 方法的 Mock

```java
// Service 方法返回 void 时，用 doThrow 模拟异常
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doNothing;

// 正常场景：void 方法默认不需要 mock（doNothing）
// 异常场景：
doThrow(new NotFoundException("会话不存在"))
        .when(sessionService).revokeSession(anyString());
```

## Controller 测试覆盖矩阵

对每个 API 端点：

| HTTP 方法 | 路径 | 正常场景 | 参数校验 | 资源不存在 | 认证失败 |
|-----------|------|---------|---------|-----------|---------|
| POST | /api/v1/xxx | 201/200 | 400 | - | 401 |
| GET | /api/v1/xxx/{id} | 200 | - | 404 | 401 |
| PUT | /api/v1/xxx/{id} | 200 | 400 | 404 | 401 |
| DELETE | /api/v1/xxx/{id} | 200 | - | 404 | 401 |

## 响应格式参考

SpecFlow 项目使用 `Result<T>` 统一响应：

```json
// 成功
{
    "code": 200,
    "message": "success",
    "data": { ... }
}

// 失败
{
    "code": 404,
    "message": "会话不存在",
    "data": null
}
```
