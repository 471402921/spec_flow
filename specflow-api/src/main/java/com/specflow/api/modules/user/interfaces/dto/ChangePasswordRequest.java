package com.specflow.api.modules.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求
 */
@Data
public class ChangePasswordRequest {

    /**
     * 当前密码
     */
    @NotBlank(message = "当前密码不能为空")
    private String oldPassword;

    /**
     * 新密码（至少8位，包含字母和数字）
     */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String newPassword;
}
