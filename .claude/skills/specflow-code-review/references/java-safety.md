# Java 类型安全规范

## 1. 空值处理

### JAVA-001: 使用 Optional 处理可能为空的返回值

```java
// ❌ 错误: 直接返回 null
public Device findById(String id) {
    Device device = deviceMapper.selectById(id);
    return device;  // 可能为 null
}

// ✅ 正确: 使用 Optional
public Optional<Device> findById(String id) {
    Device device = deviceMapper.selectById(id);
    return Optional.ofNullable(device);
}

// 调用方
Device device = deviceService.findById(id)
        .orElseThrow(() -> new DeviceNotFoundException(id));
```

### JAVA-002: 使用 @NonNull/@Nullable 注解

```java
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

// ✅ 正确: 明确标注参数和返回值的空值约定
public class DeviceService {

    @NonNull
    public Device createDevice(@NonNull CreateDeviceRequest request) {
        // 参数不能为 null，返回值也不会为 null
    }

    @Nullable
    public Device findBySerialNumber(@NonNull String serialNumber) {
        // 参数不能为 null，但返回值可能为 null
    }
}
```

### JAVA-003: 避免在集合中使用 null

```java
// ❌ 错误: 集合中可能包含 null
List<Device> devices = getDevices();
devices.forEach(d -> d.getName());  // 可能 NPE

// ✅ 正确: 使用空集合代替 null
public List<Device> getDevices() {
    List<Device> devices = deviceRepository.findAll();
    return devices != null ? devices : Collections.emptyList();
}

// ✅ 正确: 过滤 null 元素
devices.stream()
    .filter(Objects::nonNull)
    .forEach(d -> d.getName());
```

### JAVA-004: 对象判空最佳实践

```java
// ❌ 错误: 直接访问可能为 null 的对象
String name = device.getOwner().getName();  // 可能 NPE

// ✅ 正确: 使用 Optional 链式调用
String name = Optional.ofNullable(device)
    .map(Device::getOwner)
    .map(User::getName)
    .orElse("Unknown");

// ✅ 正确: 提前校验
if (device == null || device.getOwner() == null) {
    throw new IllegalArgumentException("Device or owner cannot be null");
}
String name = device.getOwner().getName();
```

## 2. 泛型使用

### JAVA-005: 避免使用原始类型

```java
// ❌ 错误: 原始类型
List devices = new ArrayList();
devices.add(new Device());
Device device = (Device) devices.get(0);  // 需要强制转换

// ✅ 正确: 使用泛型
List<Device> devices = new ArrayList<>();
devices.add(new Device());
Device device = devices.get(0);  // 无需强制转换
```

### JAVA-006: 泛型方法和类

```java
// ✅ 正确: 泛型方法
public <T> T getFirst(List<T> list) {
    if (list == null || list.isEmpty()) {
        return null;
    }
    return list.get(0);
}

// ✅ 正确: 有界泛型
public <T extends Comparable<T>> T max(T a, T b) {
    return a.compareTo(b) > 0 ? a : b;
}

// ✅ 正确: 泛型接口
public interface Repository<T, ID> {
    Optional<T> findById(ID id);
    void save(T entity);
    void delete(ID id);
}
```

### JAVA-007: 通配符使用

```java
// 生产者使用 extends
public void processDevices(List<? extends Device> devices) {
    for (Device device : devices) {
        // 只读操作
    }
}

// 消费者使用 super
public void addDevices(List<? super SmartDevice> devices) {
    devices.add(new SmartDevice());  // 写操作
}

// PECS 原则: Producer Extends, Consumer Super
```

## 3. 类型转换

### JAVA-008: 安全的类型转换

```java
// ❌ 错误: 直接强制转换
Object obj = getData();
Device device = (Device) obj;  // 可能 ClassCastException

// ✅ 正确: 使用 instanceof 检查
Object obj = getData();
if (obj instanceof Device device) {  // Java 16+ 模式匹配
    // 直接使用 device
}

// ✅ 正确: 传统方式
if (obj instanceof Device) {
    Device device = (Device) obj;
}
```

### JAVA-009: 避免不必要的装箱拆箱

```java
// ❌ 错误: 频繁装箱拆箱
List<Integer> numbers = new ArrayList<>();
int sum = 0;
for (Integer number : numbers) {
    sum += number;  // 每次都拆箱
}

// ✅ 正确: 使用原始类型流
int sum = numbers.stream()
    .mapToInt(Integer::intValue)
    .sum();

// ✅ 正确: 使用原始类型数组
int[] primitiveArray = numbers.stream()
    .mapToInt(Integer::intValue)
    .toArray();
```

## 4. 枚举使用

### JAVA-010: 使用枚举代替魔法值

```java
// ❌ 错误: 魔法字符串
public void setStatus(String status) {
    if ("online".equals(status)) {
        // ...
    }
}

// ✅ 正确: 使用枚举
public enum DeviceStatus {
    ONLINE,
    OFFLINE,
    UNKNOWN;
}

public void setStatus(DeviceStatus status) {
    if (status == DeviceStatus.ONLINE) {
        // 类型安全
    }
}
```

### JAVA-011: 枚举高级用法

```java
// ✅ 正确: 枚举包含属性和方法
public enum DeviceStatus {
    ONLINE(1, "在线"),
    OFFLINE(2, "离线"),
    UNKNOWN(0, "未知");

    private final int code;
    private final String description;

    DeviceStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static DeviceStatus fromCode(int code) {
        for (DeviceStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
```

## 5. 不可变性

### JAVA-012: 使用不可变对象

```java
// ❌ 错误: 可变对象
public class DeviceId {
    private String value;

    public void setValue(String value) {
        this.value = value;
    }
}

// ✅ 正确: 不可变对象（Java 16+ Record）
public record DeviceId(String value) {
    public DeviceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeviceId cannot be empty");
        }
    }
}

// ✅ 正确: 传统不可变类
public final class DeviceId {
    private final String value;

    public DeviceId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DeviceId cannot be empty");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
```

### JAVA-013: 使用不可变集合

```java
// ❌ 错误: 暴露可变集合
public class Device {
    private List<String> tags = new ArrayList<>();

    public List<String> getTags() {
        return tags;  // 外部可修改
    }
}

// ✅ 正确: 返回不可变视图
public List<String> getTags() {
    return Collections.unmodifiableList(tags);
}

// ✅ 正确: 返回防御性副本
public List<String> getTags() {
    return new ArrayList<>(tags);
}

// ✅ 正确: 使用 Java 9+ 工厂方法
List<String> immutableList = List.of("tag1", "tag2");
Set<String> immutableSet = Set.of("a", "b", "c");
Map<String, Integer> immutableMap = Map.of("key", 1);
```

## 6. 字符串处理

### JAVA-014: 字符串比较

```java
// ❌ 错误: 可能 NPE
if (status.equals("online")) {
    // status 可能为 null
}

// ✅ 正确: 常量在前
if ("online".equals(status)) {
    // 安全
}

// ✅ 正确: 使用 Objects.equals
if (Objects.equals(status, "online")) {
    // 安全处理 null
}

// ✅ 更好: 使用枚举
if (status == DeviceStatus.ONLINE) {
    // 类型安全
}
```

### JAVA-015: 字符串拼接

```java
// ❌ 错误: 循环中使用 + 拼接
String result = "";
for (String item : items) {
    result += item + ",";  // 每次创建新对象
}

// ✅ 正确: 使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (String item : items) {
    sb.append(item).append(",");
}
String result = sb.toString();

// ✅ 正确: 使用 String.join
String result = String.join(",", items);

// ✅ 正确: 使用 Stream
String result = items.stream()
    .collect(Collectors.joining(","));
```

## 7. 日期时间处理

### JAVA-016: 使用 java.time API

```java
// ❌ 错误: 使用已弃用的 Date
Date now = new Date();
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");  // 非线程安全

// ✅ 正确: 使用 java.time
Instant now = Instant.now();
LocalDateTime localNow = LocalDateTime.now();
ZonedDateTime zonedNow = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));

// 格式化（线程安全）
DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
String formatted = localNow.format(formatter);
```

### JAVA-017: 时间计算

```java
// ✅ 正确: 使用 Duration 和 Period
Duration duration = Duration.between(start, end);
long minutes = duration.toMinutes();

// 检查是否超时
if (Duration.between(lastHeartbeat, Instant.now()).toMinutes() > 5) {
    // 已超时
}

// 日期加减
LocalDate nextWeek = LocalDate.now().plusWeeks(1);
LocalDateTime twoHoursLater = LocalDateTime.now().plusHours(2);
```

## 8. 集合操作

### JAVA-018: 集合初始化

```java
// ✅ 正确: 使用工厂方法
List<String> list = List.of("a", "b", "c");
Set<String> set = Set.of("a", "b", "c");
Map<String, Integer> map = Map.of("a", 1, "b", 2);

// ✅ 正确: 可变集合初始化
List<String> mutableList = new ArrayList<>(List.of("a", "b"));
Map<String, Integer> mutableMap = new HashMap<>(Map.of("a", 1));

// ✅ 正确: 指定初始容量
List<String> largeList = new ArrayList<>(1000);
Map<String, Integer> largeMap = new HashMap<>(1000);
```

### JAVA-019: Stream 使用

```java
// ✅ 正确: 使用 Stream 处理集合
List<String> names = devices.stream()
    .filter(d -> d.getStatus() == DeviceStatus.ONLINE)
    .map(Device::getName)
    .distinct()
    .sorted()
    .collect(Collectors.toList());

// ✅ 正确: 并行流（注意线程安全）
long count = devices.parallelStream()
    .filter(Device::isOnline)
    .count();

// ✅ 正确: 分组
Map<DeviceStatus, List<Device>> byStatus = devices.stream()
    .collect(Collectors.groupingBy(Device::getStatus));
```
