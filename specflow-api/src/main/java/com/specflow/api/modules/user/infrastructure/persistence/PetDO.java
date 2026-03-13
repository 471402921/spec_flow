package com.specflow.api.modules.user.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Pet 数据对象（DO）- Infrastructure 层
 *
 * <p>职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("pets")
public class PetDO {

    /**
     * 宠物 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 主人 ID
     */
    private String ownerId;

    /**
     * 名字
     */
    private String name;

    /**
     * 种类（DOG/CAT）
     */
    private String species;

    /**
     * 品种
     */
    private String breed;

    /**
     * 性别（MALE/FEMALE）
     */
    private String gender;

    /**
     * 生日
     */
    private LocalDate birthday;

    /**
     * 头像 URL
     */
    private String avatarUrl;

    /**
     * 软删除标记
     */
    private Boolean deleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
