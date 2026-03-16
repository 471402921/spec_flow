# 常见测试陷阱与解决方案

本文档记录 SpecFlow Service 项目在测试开发中遇到的实际问题，均来自真实踩坑经验。

---

## PITFALL-001: @Value 在 @InjectMocks 中不生效

**现象**: 单元测试中 `@Value` 注入的字段为 0（int）或 null（String）

**根因**: `@InjectMocks` 使用 Mockito 创建对象，不走 Spring 容器，`@Value` 注解不会被处理

**解决方案**:
```java
@BeforeEach
void setUp() {
    // 使用 ReflectionTestUtils 手动注入
    ReflectionTestUtils.setField(orderService, "orderExpirationDays", 30);
}
```

**实际案例**: `OrderServiceTest` 中 `orderExpirationDays` 为 0，导致 `Order.create()` 创建的订单立即过期

---

## PITFALL-002: Lombok Boolean vs boolean Getter 命名

**现象**: 将字段从 `Boolean` 改为 `boolean` 后，编译报错 "找不到符号 getRevoked()"

**根因**: Lombok @Data 对不同类型生成不同的 getter：
- `Boolean revoked` → `getRevoked()`
- `boolean revoked` → `isRevoked()`

**解决方案**: 修改字段类型后，全局搜索替换 getter 调用

```bash
# 查找所有调用点
grep -r "getRevoked" --include="*.java" specflow-api/src/
# 替换为
grep -r "isRevoked" --include="*.java" specflow-api/src/
```

**影响范围**（Order 模块实际修改）:
- `OrderService.java` (1 处)
- `OrderConverter.java` (1 处)
- `OrderResponse.java` (1 处)
- `OrderServiceTest.java` (2 处)

**最佳实践**: Domain Entity 优先使用 primitive boolean，避免 NPE 风险

---

## PITFALL-003: 时区导致测试不稳定

**现象**: 测试在不同机器上偶尔失败，涉及日期/时间比较

**根因**: 使用 `ZoneId.systemDefault()` 导致不同环境结果不同

**解决方案**:
```java
// ❌ 错误
LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant();

// ✅ 正确
LocalDateTime.now().atZone(ZoneId.of("UTC")).toInstant();
```

**在 Converter 中**:
```java
// OrderConverter.java
public static Order toDomain(OrderDO dataObject) {
    order.setExpiredAt(dataObject.getExpiredAt()
            .atZone(ZoneId.of("UTC")).toInstant());  // 明确 UTC
}
```

---

## PITFALL-004: @WebMvcTest 加载不到 GlobalExceptionHandler

**现象**: Controller 测试中，异常未被正确转换为错误响应，返回 500 而非预期的 404/401

**根因**: `@WebMvcTest` 只加载指定 Controller，不会自动加载 `@RestControllerAdvice`

**解决方案**:
```java
// 方式 1: 在测试类上显式导入
@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest { }

// 方式 2: 如果 GlobalExceptionHandler 在同一个包扫描路径下，
// @WebMvcTest 会自动扫描 @ControllerAdvice（Spring Boot 默认行为）
// 确保 GlobalExceptionHandler 使用了 @RestControllerAdvice 注解
```

**验证**: 如果异常场景返回了预期状态码，说明 Handler 已正确加载

---

## PITFALL-005: Mock void 方法

**现象**: Service 的 void 方法（如 `cancelOrder`）需要在异常场景中抛出异常，但 `when().thenThrow()` 语法不适用

**根因**: `when(mock.method()).thenThrow()` 不适用于 void 方法

**解决方案**:
```java
// ✅ 正确：使用 doThrow
doThrow(new NotFoundException("订单不存在"))
        .when(orderService).cancelOrder(anyString());

// ❌ 错误：void 方法不能用 when().thenThrow()
when(orderService.cancelOrder(anyString()))
        .thenThrow(new NotFoundException("订单不存在"));
// 编译错误：when() 需要非 void 返回值
```

---

## PITFALL-006: 测试间状态泄漏

**现象**: 单独运行测试通过，但一起运行时某些测试失败

**根因**: 测试之间共享了可变状态（如修改了 Mock 对象的属性）

**解决方案**:
```java
// ✅ 正确：每个测试使用独立的数据
@BeforeEach
void setUp() {
    // 每次都创建新的 mock 数据
    mockOrder = Order.create("user-123", "token_abc", 30);
}

// ❌ 错误：在类级别共享可变对象
private Order mockOrder = Order.create("user-123", "token_abc", 30);
// 如果某个测试调用了 mockOrder.cancel()，其他测试也会受影响
```

---

## PITFALL-007: AssertJ vs JUnit 断言混用

**现象**: 代码风格不一致，部分测试使用 `assertEquals`，部分使用 `assertThat`

**项目规范**: 统一使用 AssertJ

```java
// ❌ JUnit 原生断言
import static org.junit.jupiter.api.Assertions.*;
assertEquals("user-123", order.getUserId());
assertTrue(order.isValid());
assertNotNull(order.getOrderNo());
assertThrows(NotFoundException.class, () -> service.getById("bad"));

// ✅ AssertJ 断言
import static org.assertj.core.api.Assertions.*;
assertThat(order.getUserId()).isEqualTo("user-123");
assertThat(order.isValid()).isTrue();
assertThat(order.getOrderNo()).isNotNull();
assertThatThrownBy(() -> service.getById("bad"))
        .isInstanceOf(NotFoundException.class);
```

**AssertJ 优势**: 链式调用、更好的错误信息、更多断言方法

---

## PITFALL-008: 测试数据硬编码 Magic Values

**现象**: 测试中随处散落 `"123"`, `"abc"`, `1` 等无意义的值

**解决方案**:
```java
// ✅ 正确：使用有意义的常量或字段
private String testUserId = "test-user-123";
private String testToken = "token_abc123def456";

@Test
void getOrder_shouldReturnOrder() {
    when(repo.findByToken(testToken)).thenReturn(Optional.of(mockOrder));
    Order result = service.getOrderById(testToken);
    assertThat(result.getUserId()).isEqualTo(testUserId);
}

// ❌ 错误：随意的值
@Test
void getOrder_shouldReturnOrder() {
    when(repo.findByToken("abc")).thenReturn(Optional.of(session));
    Order result = service.getOrderById("abc");
    assertThat(result.getUserId()).isEqualTo("u1");
}
```

---

## PITFALL-009: 缺少 @DisplayName 导致报告不可读

**现象**: 测试失败时，报告只显示方法名，不易理解

**解决方案**:
```java
// ✅ 正确：中文 DisplayName，失败时一眼看懂
@Test
@DisplayName("验证订单 - 订单已过期时应抛出业务异常")
void validateOrder_whenOrderIsExpired_shouldThrowBusinessException() { }

// ❌ 错误：没有 DisplayName
@Test
void validateOrder_whenOrderIsExpired_shouldThrowBusinessException() { }
// 报告中只显示长长的方法名
```

---

## PITFALL-010: 忘记验证 Repository 交互

**现象**: 测试通过了，但实际上 Service 并没有调用 Repository 保存数据

**解决方案**:
```java
@Test
void createOrder_shouldSaveToRepository() {
    Order result = orderService.createOrder(testUserId);

    // 不仅验证返回值
    assertThat(result).isNotNull();

    // 还要验证 Repository 被调用了
    verify(orderRepository).save(any(Order.class));
}
```

**关键写操作必须 verify**:
- `save()` / `insert()`
- `update()` / `updateById()`
- `delete()` / `deleteById()`

---

## 速查表

| 陷阱 | 症状 | 一句话方案 |
|------|------|----------|
| PITFALL-001 | @Value 字段为 0/null | `ReflectionTestUtils.setField()` |
| PITFALL-002 | 找不到 getXxx() | Boolean→isXxx(), boolean→isXxx() |
| PITFALL-003 | 日期测试不稳定 | 统一 `ZoneId.of("UTC")` |
| PITFALL-004 | 异常返回 500 | `@Import(GlobalExceptionHandler.class)` |
| PITFALL-005 | void 方法 mock 报错 | `doThrow().when()` 代替 `when().thenThrow()` |
| PITFALL-006 | 批量运行失败 | `@BeforeEach` 中重建数据 |
| PITFALL-007 | 断言风格不统一 | 统一使用 AssertJ |
| PITFALL-008 | Magic values | 使用有意义的测试常量 |
| PITFALL-009 | 报告不可读 | 加 `@DisplayName` 中文描述 |
| PITFALL-010 | 漏验交互 | `verify(repo).save()` |
