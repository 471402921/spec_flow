# 代码-文档一致性检查规则

> 适用于模式 D：检查代码实现与 Tech Pack 文档定义是否一致。

## D1. API 接口一致性

### 检查项

| 检查项 | 方法 | 示例 |
|--------|------|------|
| API 路径一致性 | 扫描 `@RequestMapping`/`@GetMapping`/`@PostMapping`/`@PutMapping`/`@DeleteMapping`，对比 Tech Pack §4 API 清单 | Tech Pack 定义 `POST /api/v1/pets`，代码实现为 `POST /api/v1/pet` → 路径不匹配 |
| HTTP 方法一致性 | 检查注解的 HTTP 方法是否与 Tech Pack 定义一致 | Tech Pack 定义 PUT，代码用 POST |
| 鉴权要求一致性 | 检查代码中 `AuthInterceptor.EXCLUDE_PATHS` 是否与 Tech Pack 鉴权定义一致 | Tech Pack 标注"需鉴权"，但代码将该路径加入了 `EXCLUDE_PATHS` |

### 执行步骤

1. 读取 Tech Pack §4 API 清单，提取所有 API 定义（路径、HTTP 方法、鉴权要求）
2. 扫描代码中的 Controller 类，提取实际实现的 API（注解路径、HTTP 方法）
3. 检查 `AuthInterceptor` 的 `EXCLUDE_PATHS`，确定哪些路径免鉴权
4. 逐一对比，输出差异

## D2. 错误码一致性

### 检查项

| 检查项 | 方法 |
|--------|------|
| 错误码定义覆盖 | 扫描代码中抛出的 `BusinessException`/`NotFoundException`/`AuthenticationException` 和对应错误码，对比 Tech Pack §4.4 错误码规范 |
| 错误码前缀一致性 | 检查是否按模块使用正确前缀（如 `USER_`、`PET_`、`SESSION_` 等） |
| 未定义的错误码 | 代码中抛出了文档未定义的错误码 |
| 未实现的错误码 | 文档定义了但代码中未使用的错误码 |

### 执行步骤

1. 读取 Tech Pack §4.4 错误码表
2. 用 `Grep` 搜索代码中的异常抛出点（`throw new BusinessException` 等）
3. 提取实际使用的错误码
4. 双向对比

## D3. 数据库实体一致性

### 检查项

| 检查项 | 方法 |
|--------|------|
| 表名一致性 | DO 类的 `@TableName` 值 vs Tech Pack §3 表名定义 |
| 字段存在性 | DO 字段是否覆盖了 Tech Pack 定义的关键字段 |
| 枚举值一致性 | Java Enum 定义值 vs Tech Pack 定义的枚举约束 |
| Flyway 迁移一致性 | `db/migration/V*.sql` 中的建表语句 vs Tech Pack §3 表结构 |

### 执行步骤

1. 读取 Tech Pack §3 数据库设计，提取表名、字段、约束
2. 扫描 `infrastructure/persistence/` 下的 DO 类
3. 扫描 `db/migration/` 下的 Flyway 迁移文件
4. 逐项对比

## D4. 代码-文档追踪

### 检查项

| 检查项 | 输出 |
|--------|------|
| 未文档化的 API | 代码中有，Tech Pack 中没有 → 需要补充文档或确认是否为临时接口 |
| 未实现的 API | Tech Pack 中有，代码中没有 → 需要实现或在文档中标注为待开发 |
| 文档过期预警 | Tech Pack 中引用的代码文件自 Tech Pack 创建后已被修改（基于 `git log`） |

### 执行步骤

1. 双向对比 API 列表（代码 vs 文档），分别列出多余和缺失
2. 对已实现的 API，检查 git 历史中 Controller 文件的最后修改时间是否晚于 Tech Pack 文件

## 输出格式

```markdown
## 代码-文档一致性检查报告

### 模块: {module}

| 检查项 | 级别 | 问题 | 代码位置 | 文档位置 | 建议 |
|--------|------|------|----------|----------|------|
| API-001 | ERROR | 路径不匹配 | PetController:37 `@PostMapping("/pet")` | tech-pack-p0.md:114 `POST /api/v1/pets` | 修改路径为 `/pets` |
| ERR-002 | WARN | 错误码未定义 | UserService:156 抛出 `INVALID_NICKNAME` | tech-pack-p0.md:229 错误码表 | 在文档中添加该错误码 |
| DB-003 | INFO | 字段未文档化 | PetDO:45 `private String color` | tech-pack-p0.md:76 pets 表定义 | 确认是否需补充到文档 |
| IMPL-004 | ERROR | API 未实现 | - | tech-pack-p0.md:118 `DELETE /api/v1/pets/{id}` | 需实现删除宠物接口 |
```

### 级别定义

| 级别 | 含义 | 处理要求 |
|------|------|---------|
| ERROR | 代码与文档存在实质性不一致，可能导致功能或安全问题 | 必须修复 |
| WARN | 文档缺失或过时，但代码实现可能是正确的 | 建议修复 |
| INFO | 信息性差异，可能是有意为之 | 确认即可 |
