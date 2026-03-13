package com.specflow.api.modules.auth.interfaces;

import com.specflow.api.modules.auth.application.SessionService;
import com.specflow.api.modules.auth.domain.entity.Session;
import com.specflow.api.modules.auth.interfaces.dto.CreateSessionRequest;
import com.specflow.api.modules.auth.interfaces.dto.SessionResponse;
import com.specflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session 管理接口
 */
@Tag(name = "Session Management", description = "会话管理相关接口")
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    /**
     * 创建新会话
     */
    @Operation(summary = "创建会话", description = "为指定用户创建新的会话令牌")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效")
    })
    @PostMapping
    public Result<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        Session session = sessionService.createSession(request.getUserId());
        return Result.success(SessionResponse.from(session));
    }

    /**
     * 查询会话详情
     */
    @Operation(summary = "查询会话", description = "根据 Token 查询会话详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    @GetMapping("/{token}")
    public Result<SessionResponse> getSession(@PathVariable String token) {
        Session session = sessionService.getSessionByToken(token);
        return Result.success(SessionResponse.from(session));
    }

    /**
     * 验证会话有效性
     */
    @Operation(summary = "验证会话", description = "验证会话令牌是否有效（未过期且未撤销）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "验证成功，会话有效"),
        @ApiResponse(responseCode = "401", description = "认证失败，会话无效")
    })
    @PostMapping("/{token}/validate")
    public Result<Boolean> validateSession(@PathVariable String token) {
        boolean valid = sessionService.validateSession(token);
        return Result.success(valid);
    }

    /**
     * 撤销会话
     */
    @Operation(summary = "撤销会话", description = "撤销指定的会话令牌")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤销成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在")
    })
    @DeleteMapping("/{token}")
    public Result<Void> revokeSession(@PathVariable String token) {
        sessionService.revokeSession(token);
        return Result.success();
    }
}
