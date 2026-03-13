# 工程架构方案定稿（DDD light for Spring Boot）
**版本：v2.0**
**状态：Accepted**

---

## 0. 结论声明（Decision）

本项目 **采用 DDD light 架构方案**，并以 **模块化单体（Modular Monolith）** 作为初始工程形态，在满足轻量化、快速迭代、团队可维护的前提下，确保未来向微服务架构演进时具备清晰、低成本的拆分路径。

> 本方案是**工程化取向的 DDD light**，而非教科书式完整 DDD。
> 技术实现基于 **Java 21 + Spring Boot 3.x + MyBatis-Plus**。

---

## 1. 采用 DDD light 的核心原因

### 1.1 项目约束与现实条件
- 团队规模：**公司 Java 团队（AI 加持）**
- 项目形态：
  - App + 设备
  - 实时通信（WebSocket）
  - 状态驱动与数据派生
  - 未来对接第三方 IoT 平台
- 架构目标：
  - 初期轻量
  - 中期可扩展
  - 后期可拆分微服务
  - 团队可维护

在上述约束下：
- **传统贫血模型 + Service 堆业务** → 中长期不可控
- **完整 DDD（CQRS / Domain Event / Context Map）** → 初期成本过高，负收益

DDD light 在当前阶段是 **性价比最高的工程解法**。

---

## 2. 本项目中 DDD light 的明确定义

### 2.1 我们"明确要做的"

#### 2.1.1 按业务能力划分模块
- 模块 ≈ 轻量 Bounded Context
- 模块边界以 **业务职责** 为准，而非技术分层
- 模块是未来**拆分微服务的最小单位**

#### 2.1.2 明确四层结构（强约束）

```
com.soulpal.api.modules.{模块名}/
├── interfaces/       # HTTP / WebSocket / DTO / 协议适配
├── application/      # UseCase / 事务边界 / 业务编排
├── domain/           # 实体 / 值对象 / 领域规则
└── infrastructure/   # DB / Cache / MQ / 外部平台
```

| 层 | 职责 | Spring 注解 |
|----|------|------------|
| interfaces | Controller、DTO、Request/Response | @RestController, @RequestBody, @Valid |
| application | UseCase、事务边界、业务编排 | @Service, @Transactional |
| domain | 实体、值对象、领域服务 | **无 Spring 注解（纯 POJO）** |
| infrastructure | 数据访问、外部调用 | @Repository, @Mapper, @Component |

#### 2.1.3 UseCase 是业务核心
- 一个 UseCase = 一个明确的业务动作
- 一个 UseCase = 一个事务边界（@Transactional）
- 所有跨实体、跨规则的业务编排 **必须** 在 UseCase 中完成

#### 2.1.4 Domain 纯净原则
- Domain **不得依赖**：
  - Spring 框架（@Autowired, @Component 等）
  - MyBatis / MyBatis-Plus 注解（@TableName, @TableId 等）
  - Redis / HTTP SDK
  - 第三方平台 SDK
- Domain 只关心：
  - 状态
  - 规则
  - 业务不变量

#### 2.1.5 模块交互规则
- 模块间：
  - 不直接访问对方的 Domain 实体
  - 通过 Application 接口或事件交互
- 跨模块引用以 **ID / Value Object** 为主

---

### 2.2 我们"明确不做的"（v1.0）

以下内容 **明确不在 v1.0 范围内**：

- 不引入 CQRS
- 不强制领域事件（Domain Event 可选）
- 不做复杂聚合一致性建模
- 不实现完整防腐层（ACL）
- 不绘制 Context Map
- 不为 DDD 理论完整性牺牲工程可读性
- 不引入 Spring Cloud 微服务治理

> 原则：**工程可控性 > 理论纯度**

---

## 3. 架构形态：模块化单体（Modular Monolith）

### 3.1 为什么是模块化单体
- 部署简单（家庭节点 + Cloudflare Tunnel + Docker Compose）
- 运维成本低
- 代码层面仍具备清晰边界
- 拆分微服务时不需要推倒重来
- 可平滑迁移到 VPS/云

### 3.2 模块化单体 ≠ 大泥球
模块化单体要求：
- 模块有清晰的 package 边界
- 模块内部可重构
- 模块之间低耦合
- 强制分层与访问约束

---

## 4. 工程目录结构

### 4.1 Maven 多模块结构

```
soulpal-service/
├── pom.xml                              # 父 POM（依赖版本管理）
├── soulpal-api/                         # API 服务模块
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   │   └── com/soulpal/api/
│       │   │       ├── SoulpalApiApplication.java
│       │   │       ├── config/          # Spring 配置类
│       │   │       ├── common/          # 通用组件（异常处理、响应封装）
│       │   │       └── modules/
│       │   │           ├── auth/        # 认证模块
│       │   │           │   ├── interfaces/
│       │   │           │   │   ├── AuthController.java
│       │   │           │   │   ├── dto/
│       │   │           │   │   │   ├── LoginRequest.java
│       │   │           │   │   │   └── LoginResponse.java
│       │   │           │   ├── application/
│       │   │           │   │   └── AuthService.java
│       │   │           │   ├── domain/
│       │   │           │   │   ├── entity/
│       │   │           │   │   │   └── Session.java
│       │   │           │   │   └── repository/
│       │   │           │   │       └── SessionRepository.java (interface)
│       │   │           │   └── infrastructure/
│       │   │           │       ├── persistence/
│       │   │           │       │   ├── SessionDO.java
│       │   │           │       │   ├── SessionMapper.java
│       │   │           │       │   └── SessionRepositoryImpl.java
│       │   │           │       └── external/
│       │   │           ├── account/
│       │   │           ├── device/
│       │   │           ├── telemetry/
│       │   │           └── notification/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       └── db/migration/        # Flyway 迁移脚本
│       └── test/
├── soulpal-worker/                      # Worker 服务模块
│   ├── pom.xml
│   └── src/
├── soulpal-common/                      # 共享代码模块
│   ├── pom.xml
│   └── src/
│       └── main/java/
│           └── com/soulpal/common/
│               ├── exception/           # 通用异常定义
│               ├── result/              # 统一响应封装
│               └── util/                # 工具类
└── deploy/                              # 部署配置
    ├── docker-compose.yml
    ├── docker-compose.dev.yml
    └── Dockerfile
```

### 4.2 模块依赖关系

```
soulpal-api ──────┬──> soulpal-common
                  │
soulpal-worker ───┘
```

---

## 5. 推荐的业务模块划分（示例）

> 模块不是一次性全部实现，而是**先定边界，逐步落地**

### 5.1 核心模块（建议优先）
- `auth`：认证、会话、Token
- `account`：用户、角色、权限
- `device`：设备、绑定、配置
- `telemetry`：设备数据消费、状态派生
- `notification`：消息、WebSocket 推送

### 5.2 可选模块（按业务推进）
- `billing`：订阅、订单、支付
- `admin`：运营与后台管理
- `integration`：外部系统/IoT 平台对接

---

## 6. 分层代码示例

### 6.1 Domain 层（纯 POJO）

```java
// domain/entity/Session.java
package com.soulpal.api.modules.auth.domain.entity;

import java.time.Instant;

public class Session {
    private String id;
    private String userId;
    private String token;
    private Instant expiresAt;
    private boolean revoked;

    // 领域行为
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    // Getters / Setters
}
```

### 6.2 Application 层（UseCase）

```java
// application/AuthService.java
package com.soulpal.api.modules.auth.application;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SessionRepository sessionRepository;
    private final AccountQueryService accountQueryService; // 跨模块接口

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. 验证用户
        Account account = accountQueryService.findByUsername(request.getUsername());
        if (!account.verifyPassword(request.getPassword())) {
            throw new AuthenticationException("Invalid credentials");
        }

        // 2. 创建会话
        Session session = Session.create(account.getId());
        sessionRepository.save(session);

        // 3. 返回结果
        return new LoginResponse(session.getToken());
    }
}
```

### 6.3 Infrastructure 层（Repository 实现）

```java
// infrastructure/persistence/SessionRepositoryImpl.java
package com.soulpal.api.modules.auth.infrastructure.persistence;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryImpl implements SessionRepository {

    private final SessionMapper sessionMapper;

    @Override
    public void save(Session session) {
        SessionDO dataObject = SessionConverter.toDataObject(session);
        sessionMapper.insert(dataObject);
    }

    @Override
    public Optional<Session> findByToken(String token) {
        SessionDO dataObject = sessionMapper.selectByToken(token);
        return Optional.ofNullable(dataObject)
                .map(SessionConverter::toDomain);
    }
}
```

---

## 7. 事务与一致性原则

### 7.1 事务边界
- 一个 UseCase 对应一个事务
- 不允许跨 UseCase 共用事务
- 使用 @Transactional 声明事务边界

### 7.2 一致性策略（v1.0）
- 模块内：强一致（DB 事务）
- 模块间：最终一致（预留事件/消息）

### 7.3 演进预留
- 预留 Outbox 模式接口
- 预留事件发布边界（Spring ApplicationEvent）
- v1.0 不强制落地

---

## 8. 与实时通信 / IoT 的适配原则

### 8.1 WebSocket 定位
- WebSocket 属于 **interfaces 层**
- 仅负责：
  - 连接管理
  - 事件推送
- 不承载核心业务规则
- 使用 Spring WebSocket + STOMP 或原生 WebSocket

### 8.2 IoT 平台演进策略
- 设备连接与时序采集：第三方 IoT 平台
- 本工程职责：
  - 数据消费
  - 状态派生
  - 业务规则
  - 实时推送

> 当 IoT 平台替换时，仅 infrastructure 层变化。

---

## 9. 对未来微服务拆分的指导意义

在本架构下，未来拆分微服务的方式是：

- 模块 → 独立 Spring Boot 应用
- Domain / Application 逻辑复用
- 接口边界自然演进为服务 API
- MyBatis Mapper 保持不变

**拆分是"物理层变化"，不是"业务重写"。**

---

## 10. 本方案的工程收益总结

- ✅ 控制早期复杂度
- ✅ 避免 Service 层腐化
- ✅ 支撑实时与状态驱动业务
- ✅ 团队可维护（Java 技术栈）
- ✅ 为微服务演进提前铺路
- ✅ AI 辅助开发友好
- ❌ 不引入不必要的架构重量

---

## 11. 最终确认声明

> 本项目工程架构 **正式采用 DDD light**，
> 并以本文件作为后续：
> - 工程框架搭建
> - 目录结构设计
> - 模块模板定义
> - 架构评审与演进
> 的 **最高约束文档之一**。

---
