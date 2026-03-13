# 代码模板

> 以下模板中的 `{placeholder}` 需根据 Tech Pack 替换为实际值。

## 1. Domain Entity

**路径**: `domain/entity/{Entity}.java`

- 纯 POJO，**无任何框架注解**
- 时间用 `Instant`，布尔用 primitive `boolean`
- 包含工厂方法 `create()` 和领域行为方法

```java
package com.specflow.api.modules.{module}.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class {Entity} {

    private String id;
    // --- Tech Pack §3 字段 ---
    private boolean deleted;
    private Instant deletedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // ==================== 领域行为 ====================

    public void softDelete() {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ==================== 工厂方法 ====================

    public static {Entity} create(/* Tech Pack §3 字段参数 */) {
        Instant now = Instant.now();
        {Entity} entity = new {Entity}();
        entity.setId(UUID.randomUUID().toString());
        // TODO: set fields from parameters
        entity.setDeleted(false);
        entity.setDeletedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
```

## 2. Domain Repository 接口

**路径**: `domain/repository/{Entity}Repository.java`

- 纯接口，无注解
- 返回 `Optional<{Entity}>` 或 `List<{Entity}>`

```java
package com.specflow.api.modules.{module}.domain.repository;

import com.specflow.api.modules.{module}.domain.entity.{Entity};

import java.util.List;
import java.util.Optional;

public interface {Entity}Repository {

    void save({Entity} entity);

    Optional<{Entity}> findById(String id);

    // --- 根据 Tech Pack §4 API 需要的查询方法 ---
}
```

## 3. Data Object (DO)

**路径**: `infrastructure/persistence/{Entity}DO.java`

- 时间用 `LocalDateTime`，布尔用 wrapper `Boolean`
- 枚举字段存为 `String`

```java
package com.specflow.api.modules.{module}.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("{table_name}")
public class {Entity}DO {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    // --- Tech Pack §3 字段（枚举用 String）---

    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 4. Converter

**路径**: `infrastructure/persistence/converter/{Entity}Converter.java`

- 静态方法，处理 `LocalDateTime` ↔ `Instant`、`String` ↔ `Enum`、`Boolean` ↔ `boolean`

```java
package com.specflow.api.modules.{module}.infrastructure.persistence.converter;

import com.specflow.api.modules.{module}.domain.entity.{Entity};
import com.specflow.api.modules.{module}.infrastructure.persistence.{Entity}DO;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class {Entity}Converter {

    public static {Entity} toDomain({Entity}DO dataObject) {
        if (dataObject == null) {
            return null;
        }
        {Entity} entity = new {Entity}();
        entity.setId(dataObject.getId());
        // TODO: convert fields (enum: toEnumType(dataObject.getXxx()))
        entity.setDeleted(
                dataObject.getDeleted() != null && dataObject.getDeleted());
        entity.setDeletedAt(toInstant(dataObject.getDeletedAt()));
        entity.setCreatedAt(toInstant(dataObject.getCreatedAt()));
        entity.setUpdatedAt(toInstant(dataObject.getUpdatedAt()));
        return entity;
    }

    public static {Entity}DO toDataObject({Entity} entity) {
        if (entity == null) {
            return null;
        }
        {Entity}DO dataObject = new {Entity}DO();
        dataObject.setId(entity.getId());
        // TODO: convert fields (enum: entity.getXxx().name())
        dataObject.setDeleted(entity.isDeleted());
        dataObject.setDeletedAt(toLocalDateTime(entity.getDeletedAt()));
        dataObject.setCreatedAt(toLocalDateTime(entity.getCreatedAt()));
        dataObject.setUpdatedAt(toLocalDateTime(entity.getUpdatedAt()));
        return dataObject;
    }

    private static Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null
                ? null
                : localDateTime.atZone(ZoneId.of("UTC")).toInstant();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null
                ? null
                : LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
    }

    // 枚举转换辅助（按需添加）
    // private static {Entity}.{EnumType} to{EnumType}(String value) {
    //     if (value == null) return null;
    //     try { return {Entity}.{EnumType}.valueOf(value); }
    //     catch (IllegalArgumentException e) { return null; }
    // }
}
```

## 5. Mapper

**路径**: `infrastructure/persistence/{Entity}Mapper.java`

```java
package com.specflow.api.modules.{module}.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface {Entity}Mapper extends BaseMapper<{Entity}DO> {

    // --- 根据 Tech Pack §4 API 需要的自定义查询 ---
    // 简单查询用 default 方法 + LambdaQueryWrapper
    // 复杂查询用 @Select 注解
}
```

## 6. Repository 实现

**路径**: `infrastructure/persistence/{Entity}RepositoryImpl.java`

- save 方法：先 update，affected == 0 再 insert

```java
package com.specflow.api.modules.{module}.infrastructure.persistence;

import com.specflow.api.modules.{module}.domain.entity.{Entity};
import com.specflow.api.modules.{module}.domain.repository.{Entity}Repository;
import com.specflow.api.modules.{module}.infrastructure.persistence.converter.{Entity}Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class {Entity}RepositoryImpl implements {Entity}Repository {

    private final {Entity}Mapper mapper;

    @Override
    public void save({Entity} entity) {
        {Entity}DO dataObject = {Entity}Converter.toDataObject(entity);
        int affected = mapper.updateById(dataObject);
        if (affected == 0) {
            mapper.insert(dataObject);
        }
    }

    @Override
    public Optional<{Entity}> findById(String id) {
        {Entity}DO dataObject = mapper.selectById(id);
        return Optional.ofNullable({Entity}Converter.toDomain(dataObject));
    }

    // --- 其他查询方法 ---
}
```

## 7. Application Service

**路径**: `application/{Entity}Service.java`

```java
package com.specflow.api.modules.{module}.application;

import com.specflow.api.modules.{module}.domain.entity.{Entity};
import com.specflow.api.modules.{module}.domain.repository.{Entity}Repository;
import com.specflow.common.exception.BusinessException;
import com.specflow.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class {Entity}Service {

    private final {Entity}Repository repository;

    // --- Tech Pack §4 API 对应的用例方法 ---
    // 每个 API 对应一个 public 方法
    // 查询方法加 @Transactional(readOnly = true)

    @Transactional(readOnly = true)
    public {Entity} get{Entity}ById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("{Entity}不存在"));
    }
}
```

## 8. Controller

**路径**: `interfaces/{Entity}Controller.java`

```java
package com.specflow.api.modules.{module}.interfaces;

import com.specflow.api.modules.{module}.application.{Entity}Service;
import com.specflow.api.modules.{module}.interfaces.dto.*;
import com.specflow.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "{Entity} Management", description = "{entity}管理相关接口")
@RestController
@RequestMapping("/api/v1/{entities}")
@RequiredArgsConstructor
public class {Entity}Controller {

    private final {Entity}Service service;

    // --- Tech Pack §4 API 清单 ---
    // POST → @ResponseStatus(HttpStatus.CREATED)，返回 Result<{Entity}Response>
    // GET → 默认 200，返回 Result<{Entity}Response> 或 Result<List<>>
    // PUT → 默认 200，返回 Result<{Entity}Response>
    // DELETE → @ResponseStatus(HttpStatus.NO_CONTENT)，返回 Result<Void>

    // 认证接口中获取当前用户：
    private String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }
}
```

## 9. Request DTO

**路径**: `interfaces/dto/{Action}{Entity}Request.java`

```java
package com.specflow.api.modules.{module}.interfaces.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class {Action}{Entity}Request {

    // --- Tech Pack §4 API 定义的请求字段 ---
    // 字符串: @NotBlank + @Size(min, max)
    // 枚举: @NotNull + 内联 Enum 定义
    // 日期: @PastOrPresent 或 @FutureOrPresent
    // 数值: @Min / @Max / @Positive
}
```

## 10. Response DTO

**路径**: `interfaces/dto/{Entity}Response.java`

```java
package com.specflow.api.modules.{module}.interfaces.dto;

import com.specflow.api.modules.{module}.domain.entity.{Entity};
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class {Entity}Response {

    // --- Tech Pack §4 API 定义的响应字段 ---
    private String id;
    private Instant createdAt;
    private Instant updatedAt;

    public static {Entity}Response fromDomain({Entity} entity) {
        if (entity == null) {
            return null;
        }
        return {Entity}Response.builder()
                .id(entity.getId())
                // TODO: map other fields
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
```

## 11. Flyway 迁移 (PostgreSQL)

**路径**: `db/migration/V{x.x}__{description}.sql`

```sql
-- =============================================
-- SpecFlow Service - {Description}
-- Version: V{x.x}
-- =============================================

CREATE TABLE {table_name} (
    id             VARCHAR(36)   PRIMARY KEY,
    -- Tech Pack §3 字段
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_{table}_deleted ON {table}(deleted);
CREATE INDEX idx_{table}_deleted_at ON {table}(deleted_at);
-- Tech Pack §3 定义的其他索引

-- 唯一约束（软删除感知）
-- CREATE UNIQUE INDEX idx_{table}_{field} ON {table}(LOWER({field}))
--     WHERE deleted = FALSE;

-- 外键（如有）
-- ALTER TABLE {table} ADD CONSTRAINT fk_{child}_{parent}
--     FOREIGN KEY ({parent_id}) REFERENCES {parent_table}(id);

-- 表注释
COMMENT ON TABLE {table_name} IS '{description}';

-- updated_at 触发器（复用已有函数）
CREATE TRIGGER {table}_updated_at_trigger
BEFORE UPDATE ON {table_name}
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();
```

## 12. H2 测试 Schema（追加到 schema-h2.sql 末尾）

```sql
-- =============================================
-- {Module} 模块表
-- =============================================

CREATE TABLE IF NOT EXISTS {table_name} (
    id             VARCHAR(36)   PRIMARY KEY,
    -- Tech Pack §3 字段
    deleted        BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at     TIMESTAMP,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- H2 索引（无 WHERE 子句，无函数索引）
CREATE INDEX IF NOT EXISTS idx_{table}_deleted
    ON {table_name}(deleted);
```

## Checkstyle 合规要点

生成代码时需注意以下 Checkstyle 规则：

| 规则 | 要求 |
|------|------|
| Import | 不使用星号导入（`import java.util.*`），按字母排序 |
| 大括号 | if/else/for/while 必须有大括号，即使单行 |
| `@Override` | 实现接口方法必须加 `@Override` |
| 方法长度 | 单方法不超过 150 行 |
| 参数个数 | 方法参数不超过 7 个 |
| 未用导入 | 不允许未使用的 import |
| 缩进 | 4 空格，无 Tab |
