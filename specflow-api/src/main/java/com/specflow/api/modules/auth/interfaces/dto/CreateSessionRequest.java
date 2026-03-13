package com.specflow.api.modules.auth.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建会话请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSessionRequest {

    /**
     * 用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;
}
