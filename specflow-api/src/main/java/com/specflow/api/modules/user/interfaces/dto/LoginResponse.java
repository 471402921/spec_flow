package com.specflow.api.modules.user.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 用户登录响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * Session Token
     */
    private String token;

    /**
     * 过期时间
     */
    private Instant expiredAt;

    /**
     * 用户信息
     */
    private UserInfo user;

    /**
     * 用户信息
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String id;
        private String email;
        private String nickname;
        private String avatarUrl;
    }
}
