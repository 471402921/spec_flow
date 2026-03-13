package com.specflow.api.modules.user.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求
 */
@Data
public class RegisterRequest {

    /**
     * 邮箱
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "请输入有效的邮箱地址")
    private String email;

    /**
     * 密码（至少8位，包含字母和数字）
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码至少8位")
    private String password;

    /**
     * 昵称（可选，2-20字符）
     */
    @Size(min = 2, max = 20, message = "昵称长度需在2-20个字符之间")
    private String nickname;
}
