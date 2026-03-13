package com.specflow.api.modules.family.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 通过邀请码加入家庭请求 DTO
 */
@Data
public class JoinFamilyRequest {

    /**
     * 邀请码（8位字符）
     */
    @NotBlank(message = "邀请码不能为空")
    private String code;
}
