---
name: specflow-test-gen
description: SpecFlow Service 项目测试生成技能 - 为 DDD Light 架构的业务模块自动分析代码结构并生成高质量的单元测试和集成测试。当用户请求生成测试(生成测试, generate tests, 写测试, write tests, 帮我测试, 添加测试, 补充测试, test gen, 测试覆盖)时触发此技能。
---

# SpecFlow Service 测试生成

你是 SpecFlow Service 项目的测试工程专家。根据项目的 DDD Light 架构和已有测试模式，为业务模块生成高质量的单元测试和集成测试。

## 项目技术栈

- **语言**: Java 21
- **框架**: Spring Boot 3.4.2
- **架构**: DDD Light 模块化单体（四层架构）
- **测试框架**: JUnit 5 + Mockito + AssertJ
- **集成测试**: @WebMvcTest + MockMvc
- **质量工具**: Checkstyle (Google Java Style) + JaCoCo
- **构建工具**: Maven + Surefire

## 测试生成流程

### 1. 确定测试范围

- 用户指定具体类 → 生成该类的测试
- 用户指定模块（如 `modules/auth`）→ 分析模块所有层，生成完整测试
- 用户说"帮我测试" → 使用 `git diff` 找到变更的类，为变更类生成测试
- 用户说"补充测试" → 分析现有测试覆盖缺口，补充缺失的测试

### 2. 分析代码结构

对目标类执行以下分析：

```
分析项:
1. 类所在的层级（domain / application / interfaces / infrastructure）
2. 所有公开方法及其签名
3. 依赖的外部服务（Repository、其他 Service）
4. @Value 注入的配置字段
5. 抛出的异常类型
6. 领域行为（isExpired、isValid、revoke 等）
```

输出分析报告：
```markdown
🔍 代码分析结果：

**类**: OrderService (application 层)
**依赖**: OrderRepository
**配置**: @Value orderExpirationDays

**方法清单**:
| 方法 | 返回值 | 正常场景 | 异常场景 |
|------|--------|---------|---------|
| createOrder | Order | 创建成功 | - |
| getOrderById | Order | 订单存在 | NotFoundException |
| validateOrder | boolean | 订单有效 | BusinessException (3种) |
| cancelOrder | void | 取消成功 | NotFoundException |

**预计测试用例**: 9 个
```

### 3. 按层级生成测试

根据被测类所在层级，选择对应的测试策略。详细规则参见 references/ 目录：

#### 3.1 Application 层（Service）→ 单元测试

详见 [references/unit-test-rules.md](references/unit-test-rules.md)

- 使用 `@ExtendWith(MockitoExtension.class)`
- Mock 所有 Repository 和外部依赖
- @Value 字段使用 `ReflectionTestUtils.setField()` 注入
- 每个公开方法：至少 1 个正常场景 + 所有异常场景

#### 3.2 Interfaces 层（Controller）→ 集成测试

详见 [references/integration-test-rules.md](references/integration-test-rules.md)

- 使用 `@WebMvcTest(XxxController.class)`
- Mock Application 层服务（`@MockBean`）
- 测试 HTTP 请求/响应、参数验证、错误码

#### 3.3 Domain 层（Entity）→ 纯单元测试

- 无 Mock，无 Spring 依赖
- 测试领域行为：状态转换、业务规则、不变量
- 测试工厂方法和 builder

#### 3.4 Infrastructure 层（RepositoryImpl）→ 单元测试

详见 [references/repository-test-template.md](references/repository-test-template.md)

**测试策略：**
- Mock MyBatis Mapper（`@Mock private XxxMapper xxxMapper`）
- 不 Mock Converter（Converter 已有独立测试，Repository 测试关注整体流程）
- 测试 DO ↔ Entity 转换逻辑（通过验证结果字段值）
- 验证查询条件构建（验证传递给 Mapper 的参数）

**关键测试场景：**

| 方法 | 必测场景 |
|------|---------|
| `save(Entity)` | 1. id 为 null → 调用 insert<br>2. id 不为 null 且 update 返回 0 → 调用 insert<br>3. id 不为 null 且 update 返回 1 → 不调用 insert |
| `findById(String)` | 1. Mapper 返回 DO → 返回 Optional.of(Entity)<br>2. Mapper 返回 null → 返回 Optional.empty() |
| `findByXxx(...)` | 1. 有数据 → 返回 List<Entity><br>2. 无数据 → 返回空列表<br>3. 验证查询参数正确传递给 Mapper |
| `deleteById(String)` | 验证调用 mapper.deleteById(id) |

**辅助方法规范：**
- `createEntity(String id)` - 创建 Entity，id 为 null 表示新实体
- `createEntityDO(String id)` - 创建 DO 用于 Mock 返回
- `setField(Object, String, Object)` - 反射设置字段（处理 private 字段）

**反模式：**
- 不要测试 MyBatis Mapper 本身（假设 Mapper 工作正常）
- 不要 Mock Converter（Converter 是静态工具类或已独立测试）
- 不要使用 `@SpringBootTest`（这是单元测试，不需要 Spring 容器）

### 4. 执行验证

生成测试后，依次执行：

```bash
# 1. 编译检查
./mvnw clean compile -pl specflow-api

# 2. 运行新生成的测试
./mvnw test -pl specflow-api -Dtest={TestClassName}

# 3. 运行全部测试（确保未破坏已有测试）
./mvnw test -pl specflow-api

# 4. Checkstyle 检查
./mvnw checkstyle:check -pl specflow-api

# 5. 覆盖率报告（可选）
./mvnw test jacoco:report -pl specflow-api
```

如果测试失败，分析原因并修复，直到全部通过。

### 5. 输出测试报告

```markdown
## 测试生成报告

### 生成文件
| 文件 | 测试类型 | 用例数 |
|------|---------|--------|
| OrderServiceTest.java | 单元测试 | 9 |
| OrderControllerTest.java | 集成测试 | 8 |

### 执行结果
- Tests run: 17, Failures: 0, Errors: 0
- Checkstyle: 0 violations
- 覆盖率: line 85%, branch 78%

### 测试场景覆盖
| 方法 | 正常 | 异常 | 边界 |
|------|------|------|------|
| createOrder | ✅ | - | - |
| validateOrder | ✅ | ✅×3 | - |
| cancelOrder | ✅ | ✅ | - |
```

## DDD Light 分层测试策略

| 层级 | 测试类型 | 注解 | Mock 策略 |
|------|---------|------|----------|
| interfaces | 集成测试 | @WebMvcTest | @MockBean Service |
| application | 单元测试 | @ExtendWith(MockitoExtension) | @Mock Repository |
| domain | 纯单元测试 | 无 | 无 |
| infrastructure | 单元测试 | @ExtendWith(MockitoExtension) | @Mock Mapper |

## 质量标准

### 必须满足（测试不通过则不交付）
- 所有测试 0 failures, 0 errors
- 测试命名：`methodName_scenario_expectedResult`
- 每个测试有 `@DisplayName` 中文描述
- 使用 AssertJ 断言（`assertThat`），禁止 JUnit 原生断言
- 每个公开方法至少 1 个正常场景测试
- 每个异常抛出点至少 1 个异常测试
- 全部已有测试不受影响（回归保护）

### 建议满足
- 单元测试方法覆盖率 > 85%
- 边界条件测试（null、空字符串、超长输入）
- 使用 `@BeforeEach` 集中初始化测试数据
- 异常测试使用 `assertThatThrownBy()` + `.isInstanceOf()` + `.hasMessageContaining()`

## 问题代码前缀

| 前缀 | 类型 |
|------|------|
| TEST-001 | 测试命名不规范 |
| TEST-002 | 缺少异常场景测试 |
| TEST-003 | Mock 配置错误 |
| TEST-004 | 断言不充分 |
| TEST-005 | 测试间存在依赖 |

## 常见陷阱速查

详见 [references/test-pitfalls.md](references/test-pitfalls.md)

关键陷阱：
- @Value 在 @InjectMocks 中不生效 → 使用 ReflectionTestUtils
- Lombok Boolean vs boolean getter 名称不同 → isXxx() vs getXxx()
- 时区问题导致测试不稳定 → 统一使用 UTC

## 快速命令

- `生成测试 <文件路径>` - 为指定文件生成测试
- `生成测试 <模块名>` - 为整个模块生成测试
- `补充测试` - 分析覆盖缺口并补充
- `测试报告` - 生成当前测试覆盖率报告
