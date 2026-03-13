package com.specflow.api.modules.family.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建家庭请求 DTO
 */
@Data
public class CreateFamilyRequest {

    /**
     * 家庭名称（2-20字符）
     */
    @NotBlank(message = "家庭名称不能为空")
    @Size(min = 2, max = 20, message = "家庭名称长度需在2-20个字符之间")
    private String name;
}
