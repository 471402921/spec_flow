package com.specflow.api.modules.user.interfaces.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改用户资料请求
 */
@Data
public class UpdateProfileRequest {

    /**
     * 昵称（2-20字符）
     */
    @Size(min = 2, max = 20, message = "昵称长度需在2-20个字符之间")
    private String nickname;

    /**
     * 头像 URL
     */
    @Size(max = 512, message = "头像URL长度不能超过512")
    private String avatarUrl;
}
