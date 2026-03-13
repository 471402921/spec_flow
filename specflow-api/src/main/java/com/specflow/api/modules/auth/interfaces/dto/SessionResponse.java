package com.specflow.api.modules.auth.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.specflow.api.modules.auth.domain.entity.Session;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 会话响应 DTO
 */
@Data
@NoArgsConstructor
public class SessionResponse {

    private String id;
    private String userId;
    private String token;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant expiredAt;

    private Boolean revoked;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant updatedAt;

    /**
     * 从 Domain Entity 创建响应对象
     */
    public static SessionResponse from(Session session) {
        SessionResponse response = new SessionResponse();
        response.id = session.getId();
        response.userId = session.getUserId();
        response.token = session.getToken();
        response.expiredAt = session.getExpiredAt();
        response.revoked = session.isRevoked();
        response.createdAt = session.getCreatedAt();
        response.updatedAt = session.getUpdatedAt();
        return response;
    }
}
