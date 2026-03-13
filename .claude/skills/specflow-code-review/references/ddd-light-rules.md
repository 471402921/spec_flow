# DDD Light 分层规则（Java / Spring Boot）

## 四层架构

```
com.specflow.api.modules.{模块名}/
├── interfaces/          # 接口层
├── application/         # 应用层
├── domain/              # 领域层
└── infrastructure/      # 基础设施层
```

## 依赖方向约束

```
Interfaces → Application → Domain
              ↓
         Infrastructure
```

## 各层允许的导入

### Domain 层 (`modules/{模块}/domain/`)

**允许导入**:
- Java 标准库 (`java.util.*`, `java.time.*` 等)
- 本模块内的其他 domain 类
- `com.specflow.common` 中的纯类型定义（VO、工具类）

**禁止导入**:
- `org.springframework.*`（任何 Spring 注解或类）
- `com.baomidou.mybatisplus.*`（MyBatis-Plus）
- `org.apache.ibatis.*`（MyBatis）
- `redis.*`, `io.lettuce.*`, `org.redisson.*`（Redis 客户端）
- `org.apache.http.*`, `okhttp3.*`（HTTP 客户端）
- 任何外部服务 SDK（`software.amazon.awssdk.*`, `com.azure.*` 等）
- 其他模块的任何代码

### Application 层 (`modules/{模块}/application/`)

**允许导入**:
- 本模块的 Domain 层
- 本模块定义的 Repository 接口（在 domain 层定义）
- `org.springframework.stereotype.Service`
- `org.springframework.transaction.annotation.Transactional`
- 其他模块的 Application 层**接口**（通过依赖注入）

**禁止导入**:
- 其他模块的 Domain 层
- 直接导入 Infrastructure 实现类（应通过接口）
- MyBatis Mapper 接口（应通过 Repository 接口）

### Infrastructure 层 (`modules/{模块}/infrastructure/`)

**允许导入**:
- 本模块的 Domain 层接口
- `org.springframework.*`
- `com.baomidou.mybatisplus.*`（MyBatis-Plus）
- Redis、HTTP 客户端、外部 SDK
- `com.specflow.common.infrastructure.*`

### Interfaces 层 (`modules/{模块}/interfaces/`)

**允许导入**:
- 本模块的 Application 层
- `org.springframework.web.*`
- `jakarta.validation.*`（Jakarta Validation）
- `io.swagger.v3.oas.annotations.*`（OpenAPI/Swagger）

**禁止导入**:
- Domain 层
- Infrastructure 层
- 其他模块的任何层

## 常见违规示例

### DDD-001: Domain 层导入 Spring 或 ORM

```java
// ❌ 错误: domain/entity/Device.java
import org.springframework.stereotype.Component;
import com.baomidou.mybatisplus.annotation.TableName;

@Component  // 错误
@TableName("device")  // 错误
public class Device {
    // ...
}

// ✅ 正确: domain/entity/Device.java
// Domain 层应该是纯 POJO，不依赖任何框架
public class Device {
    private String id;
    private String serialNumber;
    private DeviceStatus status;

    // 领域行为
    public boolean isOnline() {
        return this.status == DeviceStatus.ONLINE;
    }

    public void activate() {
        this.status = DeviceStatus.ONLINE;
    }
}
```

### DDD-002: Controller 直接调用 Mapper/Repository

```java
// ❌ 错误: interfaces/DeviceController.java
@RestController
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceMapper deviceMapper;  // 直接注入 Mapper

    @GetMapping("/devices/{id}")
    public Device getDevice(@PathVariable String id) {
        return deviceMapper.selectById(id);  // 直接调用 Mapper
    }
}

// ✅ 正确: interfaces/DeviceController.java
@RestController
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceQueryService deviceQueryService;  // 注入 Application 层服务

    @GetMapping("/devices/{id}")
    public DeviceResponse getDevice(@PathVariable String id) {
        Device device = deviceQueryService.findById(id);
        return DeviceResponse.from(device);
    }
}
```

### DDD-003: 跨模块直接访问 Domain

```java
// ❌ 错误: modules/notification/application/NotificationService.java
import com.specflow.api.modules.device.domain.entity.Device;

public class NotificationService {
    public void notifyDevice(Device device) {  // 直接传递其他模块实体
        // ...
    }
}

// ✅ 正确: 使用 ID 引用
public class NotificationService {
    public void notifyDevice(String deviceId) {  // 使用 ID 引用
        // 如果需要设备信息，通过 Application 层接口获取
    }
}
```

### DDD-004: Domain 层使用 @Autowired 或 @Service

```java
// ❌ 错误: domain/service/DeviceDomainService.java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DeviceDomainService {
    @Autowired
    private DeviceRepository deviceRepository;
}

// ✅ 正确: domain/service/DeviceDomainService.java
// Domain 服务应该是纯类，通过构造函数接收依赖
public class DeviceDomainService {

    public boolean validateSerialNumber(String serialNumber) {
        // 纯业务逻辑，无外部依赖
        return serialNumber != null
            && serialNumber.length() >= 10
            && serialNumber.matches("[A-Z0-9]+");
    }

    public DeviceStatus calculateStatus(Device device, Instant lastHeartbeat) {
        // 纯业务规则
        if (lastHeartbeat == null) {
            return DeviceStatus.UNKNOWN;
        }
        Duration elapsed = Duration.between(lastHeartbeat, Instant.now());
        return elapsed.toMinutes() < 5 ? DeviceStatus.ONLINE : DeviceStatus.OFFLINE;
    }
}
```

### DDD-005: Application 层缺少事务边界

```java
// ❌ 错误: 涉及多个写操作但没有事务
@Service
@RequiredArgsConstructor
public class BindDeviceService {
    public void bindDevice(BindDeviceCommand command) {
        deviceRepository.save(device);  // 写操作 1
        bindingRepository.save(binding);  // 写操作 2 - 如果失败，数据不一致
    }
}

// ✅ 正确: 使用 @Transactional 声明事务边界
@Service
@RequiredArgsConstructor
public class BindDeviceService {

    @Transactional
    public void bindDevice(BindDeviceCommand command) {
        deviceRepository.save(device);
        bindingRepository.save(binding);
        // 两个操作在同一事务中
    }
}
```

## UseCase / Service 规范

- 一个 Service 方法 = 一个明确的业务动作
- 一个 Service 方法 = 一个事务边界（@Transactional）
- 命名建议: `<动词><名词>Service`，如 `BindDeviceService`，或直接使用 `DeviceService` 配合方法命名

```java
// application/DeviceService.java
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceEventPublisher eventPublisher;

    @Transactional
    public Device bindDevice(BindDeviceCommand command) {
        // 1. 验证业务规则
        if (deviceRepository.existsBySerialNumber(command.getSerialNumber())) {
            throw new DeviceAlreadyBoundException(command.getSerialNumber());
        }

        // 2. 创建领域对象
        Device device = Device.create(
            command.getSerialNumber(),
            command.getUserId()
        );

        // 3. 持久化
        deviceRepository.save(device);

        // 4. 发布领域事件（可选）
        eventPublisher.publish(new DeviceBoundEvent(device.getId()));

        // 5. 返回结果
        return device;
    }
}
```

## Repository 接口定义规范

Repository 接口应定义在 Domain 层，实现在 Infrastructure 层：

```java
// domain/repository/DeviceRepository.java（接口定义）
public interface DeviceRepository {
    Optional<Device> findById(String id);
    Optional<Device> findBySerialNumber(String serialNumber);
    boolean existsBySerialNumber(String serialNumber);
    void save(Device device);
    void delete(String id);
}

// infrastructure/persistence/DeviceRepositoryImpl.java（实现）
@Repository
@RequiredArgsConstructor
public class DeviceRepositoryImpl implements DeviceRepository {

    private final DeviceMapper deviceMapper;

    @Override
    public Optional<Device> findById(String id) {
        DeviceDO dataObject = deviceMapper.selectById(id);
        return Optional.ofNullable(dataObject)
                .map(DeviceConverter::toDomain);
    }

    @Override
    public void save(Device device) {
        DeviceDO dataObject = DeviceConverter.toDataObject(device);
        if (device.getId() == null) {
            deviceMapper.insert(dataObject);
        } else {
            deviceMapper.updateById(dataObject);
        }
    }
}
```
