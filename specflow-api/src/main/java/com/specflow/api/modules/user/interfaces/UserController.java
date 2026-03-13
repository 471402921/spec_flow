package com.specflow.api.modules.user.interfaces;

import com.specflow.api.modules.user.application.UserService;
import com.specflow.api.modules.user.domain.service.TokenProvider;
import com.specflow.api.modules.user.domain.entity.User;
import com.specflow.api.modules.user.interfaces.dto.ChangePasswordRequest;
import com.specflow.api.modules.user.interfaces.dto.LoginRequest;
import com.specflow.api.modules.user.interfaces.dto.LoginResponse;
import com.specflow.api.modules.user.interfaces.dto.RegisterRequest;
import com.specflow.api.modules.user.interfaces.dto.UpdateProfileRequest;
import com.specflow.api.modules.user.interfaces.dto.UserResponse;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理控制器
 */
@Tag(name = "User Management", description = "用户管理相关接口")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final TokenProvider tokenProvider;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "使用邮箱和密码注册新用户")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "注册成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "409", description = "邮箱已被注册")
    })
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.getEmail(),
                request.getPassword(),
                request.getNickname()
        );
        return Result.success(UserResponse.fromDomain(user));
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "使用邮箱和密码登录，返回 Session Token")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "401", description = "邮箱或密码错误")
    })
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getEmail(), request.getPassword());
        String userId = tokenProvider.getUserIdByToken(token);
        User user = userService.getUserById(userId);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );

        LoginResponse response = new LoginResponse(
                token,
                tokenProvider.getExpiredAtByToken(token),
                userInfo
        );

        return Result.success(response);
    }

    /**
     * 用户登出
     */
    @Operation(summary = "用户登出", description = "使当前 Session Token 失效")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登出成功"),
        @ApiResponse(responseCode = "401", description = "未提供有效令牌")
    })
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("缺少认证令牌");
        }
        userService.logout(token);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @GetMapping("/me")
    public Result<UserResponse> getCurrentUser(HttpServletRequest request) {
        String userId = getCurrentUserId(request);
        User user = userService.getUserById(userId);
        return Result.success(UserResponse.fromDomain(user));
    }

    /**
     * 修改用户资料
     */
    @Operation(summary = "修改用户资料", description = "修改当前用户的昵称和头像")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "请求参数无效"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/me")
    public Result<UserResponse> updateProfile(
            HttpServletRequest request,
            @Valid @RequestBody UpdateProfileRequest updateRequest) {
        String userId = getCurrentUserId(request);
        User user = userService.updateProfile(
                userId,
                updateRequest.getNickname(),
                updateRequest.getAvatarUrl()
        );
        return Result.success(UserResponse.fromDomain(user));
    }

    /**
     * 修改密码
     */
    @Operation(summary = "修改密码", description = "验证当前密码后修改为新密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "当前密码错误或新密码格式无效"),
        @ApiResponse(responseCode = "401", description = "未授权")
    })
    @PutMapping("/me/password")
    public Result<Void> changePassword(
            HttpServletRequest request,
            @Valid @RequestBody ChangePasswordRequest passwordRequest) {
        String userId = getCurrentUserId(request);
        userService.changePassword(
                userId,
                passwordRequest.getOldPassword(),
                passwordRequest.getNewPassword()
        );
        return Result.success();
    }

    // ==================== 私有辅助方法 ====================

    private String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
