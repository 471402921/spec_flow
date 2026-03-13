package com.specflow.api.modules.user.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 编辑宠物请求
 */
@Data
public class UpdatePetRequest {

    /**
     * 名字（1-30字符）
     */
    @NotBlank(message = "宠物名字不能为空")
    @Size(min = 1, max = 30, message = "宠物名字长度需在1-30个字符之间")
    private String name;

    /**
     * 种类（DOG/CAT）
     */
    @NotNull(message = "宠物种类不能为空")
    private Species species;

    /**
     * 品种（1-50字符）
     */
    @NotBlank(message = "宠物品种不能为空")
    @Size(min = 1, max = 50, message = "品种长度需在1-50个字符之间")
    private String breed;

    /**
     * 性别（MALE/FEMALE）
     */
    @NotNull(message = "宠物性别不能为空")
    private Gender gender;

    /**
     * 生日（不能晚于今天）
     */
    @PastOrPresent(message = "生日不能晚于今天")
    private LocalDate birthday;

    /**
     * 头像 URL
     */
    @Size(max = 512, message = "头像URL长度不能超过512")
    private String avatarUrl;

    /**
     * 宠物种类枚举
     */
    public enum Species {
        DOG, CAT
    }

    /**
     * 宠物性别枚举
     */
    public enum Gender {
        MALE, FEMALE
    }
}
