package com.specflow.api.modules.family.interfaces.dto;

import com.specflow.api.modules.family.domain.entity.FamilyInvitation;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 家庭邀请码响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyInvitationResponse {

    /**
     * 邀请码
     */
    private String code;

    /**
     * 过期时间
     */
    private Instant expiredAt;

    /**
     * 创建时间
     */
    private Instant createdAt;

    /**
     * 从领域实体转换
     */
    public static FamilyInvitationResponse fromDomain(FamilyInvitation invitation) {
        if (invitation == null) {
            return null;
        }
        return new FamilyInvitationResponse(
                invitation.getCode(),
                invitation.getExpiredAt(),
                invitation.getCreatedAt()
        );
    }
}
