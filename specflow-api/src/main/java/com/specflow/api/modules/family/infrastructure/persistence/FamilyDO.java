package com.specflow.api.modules.family.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Family 数据对象（DO）- Infrastructure 层
 *
 * <p>职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("families")
public class FamilyDO {

    /**
     * 家庭 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 家庭名称（2-20字符）
     */
    private String name;

    /**
     * 当前家庭主人 ID（冗余字段）
     */
    private String ownerId;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
