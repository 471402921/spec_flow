package com.specflow.api.modules.family.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 转让主人身份请求 DTO
 */
@Data
public class TransferOwnershipRequest {

    /**
     * 新主人用户 ID
     */
    @NotBlank(message = "新主人用户ID不能为空")
    private String newOwnerId;
}
