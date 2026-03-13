package com.specflow.api.modules.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session 数据对象（DO）- Infrastructure 层
 *
 * 职责：
 * - 承载 MyBatis-Plus 框架注解
 * - 对接数据库表结构
 * - 使用 LocalDateTime 对接 PostgreSQL TIMESTAMP
 *
 * 与 Domain Entity 的区别：
 * - DO 包含框架注解，Entity 是纯 POJO
 * - DO 使用 LocalDateTime，Entity 使用 Instant
 * - DO 仅在 Infrastructure 层使用，Entity 在整个应用层流转
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("session")
public class SessionDO {

    /**
     * 会话 ID（UUID 自动生成）
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 会话令牌
     */
    private String token;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 是否已撤销
     */
    private Boolean revoked;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
