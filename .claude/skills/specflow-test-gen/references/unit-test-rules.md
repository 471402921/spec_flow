# 单元测试规则（Application 层 + Domain 层）

## Application 层单元测试模板

```java
package com.specflow.api.modules.{module}.application;

import com.specflow.api.modules.{module}.domain.entity.{Entity};
import com.specflow.api.modules.{module}.domain.repository.{Entity}Repository;
import com.specflow.common.exception.AuthenticationException;
import com.specflow.common.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("{Service} 单元测试")
class {Service}Test {

    @Mock
    private {Entity}Repository {entity}Repository;

    @InjectMocks
    private {Service} {service};

    @BeforeEach
    void setUp() {
        // 注入 @Value 字段（单元测试中 @Value 不生效）
        // ReflectionTestUtils.setField({service}, "fieldName", value);

        // 初始化公共测试数据
    }

    // --- 正常场景 ---

    @Test
    @DisplayName("创建XX - 正常创建应返回有效对象")
    void createXxx_shouldReturnValidEntity() {
        // Given - 准备测试数据

        // When - 执行
        {Entity} result = {service}.createXxx(param);

        // Then - 验证返回值
        assertThat(result).isNotNull();
        assertThat(result.getField()).isEqualTo(expected);

        // Then - 验证交互
        verify({entity}Repository).save(any({Entity}.class));
    }

    // --- 异常场景 ---

    @Test
    @DisplayName("查询XX - 不存在时应抛出 NotFoundException")
    void getXxx_whenNotExists_shouldThrowNotFoundException() {
        // Given
        when({entity}Repository.findById(anyString()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> {service}.getXxx("non-existent-id"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("不存在");
    }
}
```

## 核心规则

### TEST-U001: @BeforeEach 集中初始化

```java
// ✅ 正确：公共数据在 setUp 中初始化
@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(orderService, "orderExpirationDays", 30);
    testUserId = "test-user-123";
    testOrderNo = "ORD-20250101-001";
    mockOrder = Order.create(testUserId, testOrderNo, 30);
}

// ❌ 错误：每个测试重复创建相同数据
@Test
void test1() {
    String userId = "test-user-123";  // 重复
    Order order = Order.create(userId, "token", 30);  // 重复
}
```

### TEST-U002: Mock 声明与注入

```java
// ✅ 正确：使用注解声明
@Mock
private OrderRepository orderRepository;

@InjectMocks
private OrderService orderService;

// ❌ 错误：手动创建 Mock
private OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
private OrderService orderService = new OrderService(orderRepository);
```

### TEST-U003: @Value 字段处理

```java
// ✅ 正确：使用 ReflectionTestUtils
@BeforeEach
void setUp() {
    ReflectionTestUtils.setField(orderService, "orderExpirationDays", 30);
}

// ❌ 错误：期望 @Value 在单元测试中自动注入
// （@InjectMocks 不走 Spring 容器，@Value 不生效）
```

### TEST-U004: verify 验证关键交互

```java
// ✅ 正确：验证 Repository 方法被调用
@Test
void cancelOrder_shouldCallSave() {
    when(orderRepository.findById(testOrderNo))
            .thenReturn(Optional.of(mockOrder));

    orderService.cancelOrder(testOrderNo);

    verify(orderRepository).findById(testOrderNo);
    verify(orderRepository).save(mockOrder);
}

// ❌ 错误：只验证返回值，不验证交互
@Test
void cancelOrder_incomplete() {
    when(orderRepository.findById(testOrderNo))
            .thenReturn(Optional.of(mockOrder));

    orderService.cancelOrder(testOrderNo);
    // 缺少 verify
}
```

### TEST-U005: 异常断言三要素

```java
// ✅ 正确：验证异常类型 + 消息内容
assertThatThrownBy(() -> orderService.cancelOrder("bad-token"))
        .isInstanceOf(NotFoundException.class)         // 类型
        .hasMessageContaining("订单不存在");             // 消息

// ❌ 错误：只验证异常类型
assertThatThrownBy(() -> orderService.cancelOrder("bad-token"))
        .isInstanceOf(Exception.class);  // 太宽泛
```

## Domain 层单元测试模板

Domain 层是纯 POJO，测试不需要 Mock 和 Spring 注解。

```java
package com.specflow.api.modules.{module}.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("{Entity} 领域实体测试")
class {Entity}Test {

    @Test
    @DisplayName("create - 工厂方法应生成有效实体")
    void create_shouldReturnValidEntity() {
        Order order = Order.create("user-123", "ORD-001", 30);

        assertThat(order.getUserId()).isEqualTo("user-123");
        assertThat(order.getOrderNo()).isEqualTo("ORD-001");
        assertThat(order.isValid()).isTrue();
        assertThat(order.isCancelled()).isFalse();
    }

    @Test
    @DisplayName("cancel - 取消后应标记为无效")
    void cancel_shouldMarkAsInvalid() {
        Order order = Order.create("user-123", "ORD-001", 30);

        order.cancel();

        assertThat(order.isCancelled()).isTrue();
        assertThat(order.isValid()).isFalse();
    }

    @Test
    @DisplayName("isExpired - 过期订单应返回 true")
    void isExpired_whenPastExpirationDate_shouldReturnTrue() {
        // 创建一个已经过期的订单（过期天数设为 -1）
        Order order = Order.create("user-123", "ORD-001", -1);

        assertThat(order.isExpired()).isTrue();
    }
}
```

## 测试场景覆盖矩阵

为每个公开方法填写：

| 方法 | 正常场景 | 异常场景 | 边界场景 |
|------|---------|---------|---------|
| createXxx | 创建成功 | - | null 参数 |
| getXxxById | 存在返回 | NotFoundException | - |
| validateXxx | 有效返回 true | AuthenticationException | 刚过期/刚取消 |
| deleteXxx | 删除成功 | NotFoundException | - |
| updateXxx | 更新成功 | NotFoundException | 部分字段更新 |

## 测试命名规则

格式：`methodName_scenario_expectedResult`

```java
// ✅ 好的命名
createOrder_shouldReturnValidOrder()
getOrderById_whenOrderExists_shouldReturnOrder()
getOrderById_whenOrderNotExists_shouldThrowNotFoundException()
validateOrder_whenOrderIsExpired_shouldThrowBusinessException()
cancelOrder_shouldSuccessfullyCancel()

// ❌ 差的命名
test1()
testCreateSession()
shouldThrowException()
```
