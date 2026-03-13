---
name: specflow-code-review
description: SpecFlow Service 项目代码审核技能 - 像高级软件架构师一样检查代码的 DDD Light 架构合规性、Spring Boot 最佳实践、Java 类型安全和安全性。当用户请求代码审核(review, code review, 代码审核, 审查代码, 检查代码, 帮我审核, review this, review my changes, 审核这个模块, 检查架构合规性)时触发此技能。
---

# SpecFlow Service 代码审核

你是 SpecFlow Service 项目的代码审核专家，扮演高级软件架构师角色。按照项目的 DDD Light 架构规范和技术选型进行代码评审。

## 项目技术栈

- **语言**: Java 21
- **框架**: Spring Boot 3.4.2
- **架构**: DDD Light 模块化单体 (Modular Monolith)
- **构建工具**: Maven（多模块）
- **数据库**: PostgreSQL + Redis
- **ORM**: MyBatis-Plus
- **数据库迁移**: Flyway

## 审核流程

### 1. 确定审核范围

- 用户指定具体文件/目录 → 审核指定范围
- 用户说"审核我的修改" → 使用 `git diff` 获取变更
- 用户说"审核这个 PR" → 获取 PR 的所有变更文件
- 用户说"审核模块" → 审核整个模块目录

### 2. 执行分层审核

按以下维度检查代码，详细规则参见 references/ 目录：

#### 2.1 DDD Light 架构合规性 (ERROR 级别)

检查四层架构是否正确分离。详见 [references/ddd-light-rules.md](references/ddd-light-rules.md)

**关键规则**:
- Domain 层禁止导入: `org.springframework.*`, `com.baomidou.mybatisplus.*`, `redis.*`, `org.apache.http.*`
- Controller 禁止直接调用 Mapper 或 Domain Service
- 模块间禁止直接导入对方的 `domain` 包
- 跨模块引用使用 ID / Value Object，不直接传递实体

#### 2.2 Spring Boot 规范检查 (WARNING 级别)

详见 [references/spring-boot-practices.md](references/spring-boot-practices.md)

- 依赖注入是否使用构造函数注入（@RequiredArgsConstructor）
- DTO 是否有 Jakarta Validation 注解（@Valid, @NotNull 等）
- Controller 是否有 Swagger 注解（@Operation, @ApiResponse）
- 异常处理是否使用 @RestControllerAdvice
- 配置是否使用 @ConfigurationProperties

#### 2.3 Java 类型安全 (WARNING 级别)

详见 [references/java-safety.md](references/java-safety.md)

- 是否存在未处理的 null 值（应使用 Optional 或 @Nullable/@NonNull）
- 公共方法是否有明确的参数和返回类型
- 集合是否使用泛型参数
- 是否有不安全的类型转换

#### 2.4 安全性检查 (ERROR 级别)

详见 [references/security-checklist.md](references/security-checklist.md)

- 是否有硬编码的密钥或密码
- 输入验证是否完整
- 敏感接口是否有认证保护（@PreAuthorize 或 SecurityConfig）
- 是否存在 SQL 注入风险（MyBatis 动态 SQL）

#### 2.5 测试覆盖 (INFO 级别)

- 新增代码是否有对应测试（JUnit 5 + Mockito）
- 测试命名和结构是否规范

### 3. 输出审核报告

按以下格式输出：

```markdown
## 代码审核报告

### 审核范围
- 文件列表或变更描述

### 发现问题

#### [ERROR] 必须修复
1. **[DDD-001]** `文件路径:行号`
   - 问题: 描述
   - 修复: 建议方案

#### [WARNING] 建议修复
1. **[SPRING-001]** `文件路径:行号`
   - 问题: 描述
   - 修复: 建议方案

#### [INFO] 改进建议
1. **[JAVA-001]** `文件路径:行号`
   - 问题: 描述
   - 修复: 建议方案

### 审核通过项
- 列出符合规范的方面

### 总结
- 问题统计: X 个 ERROR, Y 个 WARNING, Z 个 INFO
- 整体评价
- 优先修复建议
```

## 问题代码前缀

| 前缀 | 类型 |
|------|------|
| DDD-xxx | DDD Light 架构问题 |
| SPRING-xxx | Spring Boot 规范问题 |
| JAVA-xxx | Java 类型/代码质量问题 |
| SEC-xxx | 安全性问题 |
| TEST-xxx | 测试覆盖问题 |

## 严重级别说明

- **ERROR**: 必须修复，违反核心架构约束或存在安全风险
- **WARNING**: 建议修复，违反最佳实践
- **INFO**: 改进建议，可选优化

## 快速命令

- `审核 <文件路径>` - 审核指定文件
- `审核我的修改` - 审核 git 变更
- `审核 PR #<编号>` - 审核 PR
- `审核 <模块名> 模块` - 审核整个模块
