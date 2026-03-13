# Spring Boot 最佳实践

## 1. 依赖注入

### SPRING-001: 使用构造函数注入

```java
// ✅ 正确: 构造函数注入（推荐使用 Lombok）
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final ConfigProperties configProperties;
}

// ✅ 正确: 显式构造函数注入
@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }
}

// ❌ 错误: 字段注入
@Service
public class DeviceService {
    @Autowired
    private DeviceRepository deviceRepository;
}
```

### SPRING-002: 使用接口解耦

```java
// ✅ 推荐: 依赖接口而非实现
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepository deviceRepository;  // 注入接口
}

// ❌ 避免: 直接依赖实现类
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceRepositoryImpl deviceRepository;  // 直接依赖实现
}
```

## 2. DTO 验证

### SPRING-003: 使用 Jakarta Validation 注解

```java
// ✅ 完整的 DTO 定义
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "创建设备请求")
public class CreateDeviceRequest {

    @Schema(description = "设备序列号", example = "SN1234567890")
    @NotBlank(message = "序列号不能为空")
    @Size(min = 10, max = 20, message = "序列号长度必须在10-20之间")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "序列号只能包含大写字母和数字")
    private String serialNumber;

    @Schema(description = "设备别名", example = "我的项圈")
    @Size(max = 50, message = "别名长度不能超过50")
    private String nickname;

    // Getters and Setters
}
```

### SPRING-004: 常用验证注解

```java
// 字符串验证
@NotNull                    // 不为 null
@NotBlank                   // 不为 null 且不为空白
@NotEmpty                   // 不为 null 且不为空（字符串/集合）
@Size(min = 1, max = 100)   // 长度/大小限制
@Pattern(regexp = "...")    // 正则匹配
@Email                      // 邮箱格式

// 数值验证
@Min(0)                     // 最小值
@Max(100)                   // 最大值
@Positive                   // 正数
@PositiveOrZero             // 非负数
@Negative                   // 负数
@DecimalMin("0.01")         // 最小小数
@DecimalMax("999.99")       // 最大小数

// 时间验证
@Past                       // 过去时间
@PastOrPresent              // 过去或现在
@Future                     // 未来时间
@FutureOrPresent            // 未来或现在

// 其他
@Valid                      // 嵌套对象验证
@AssertTrue                 // 必须为 true
@AssertFalse                // 必须为 false
```

## 3. Controller 规范

### SPRING-005: Swagger/OpenAPI 文档注解

```java
@Tag(name = "Devices", description = "设备管理接口")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "绑定新设备", description = "将设备绑定到当前用户")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "绑定成功",
            content = @Content(schema = @Schema(implementation = DeviceResponse.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未认证"),
        @ApiResponse(responseCode = "409", description = "设备已被绑定")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceResponse bindDevice(
            @Valid @RequestBody CreateDeviceRequest request,
            @AuthenticationPrincipal UserDetails user) {
        Device device = deviceService.bindDevice(request, user.getUsername());
        return DeviceResponse.from(device);
    }

    @Operation(summary = "获取设备详情")
    @GetMapping("/{id}")
    public DeviceResponse getDevice(
            @Parameter(description = "设备ID") @PathVariable String id) {
        return deviceService.findById(id)
                .map(DeviceResponse::from)
                .orElseThrow(() -> new DeviceNotFoundException(id));
    }
}
```

### SPRING-006: 响应封装

```java
// ✅ 推荐: 使用统一响应封装
@Data
@AllArgsConstructor
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, Instant.now());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, Instant.now());
    }
}

// Controller 中使用
@GetMapping("/{id}")
public ApiResponse<DeviceResponse> getDevice(@PathVariable String id) {
    DeviceResponse response = deviceService.getDevice(id);
    return ApiResponse.success(response);
}
```

## 4. 异常处理

### SPRING-007: 使用 @RestControllerAdvice

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFound(DeviceNotFoundException ex) {
        return ApiResponse.error(404, ex.getMessage());
    }

    @ExceptionHandler(DeviceAlreadyBoundException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<Void> handleConflict(DeviceAlreadyBoundException ex) {
        return ApiResponse.error(409, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                    FieldError::getField,
                    FieldError::getDefaultMessage,
                    (a, b) -> a
                ));
        return ApiResponse.error(400, "参数验证失败", errors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("Unexpected error", ex);
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

### SPRING-008: 自定义业务异常

```java
// 定义异常基类
public abstract class BusinessException extends RuntimeException {
    private final int code;

    protected BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

// 具体业务异常
public class DeviceNotFoundException extends BusinessException {
    public DeviceNotFoundException(String deviceId) {
        super(404, "设备不存在: " + deviceId);
    }
}

public class DeviceAlreadyBoundException extends BusinessException {
    public DeviceAlreadyBoundException(String serialNumber) {
        super(409, "设备 " + serialNumber + " 已被其他用户绑定");
    }
}
```

## 5. 配置管理

### SPRING-009: 使用 @ConfigurationProperties

```java
// ✅ 正确: 类型安全的配置
@Configuration
@ConfigurationProperties(prefix = "app.device")
@Validated
@Data
public class DeviceProperties {

    @NotNull
    private Duration heartbeatTimeout = Duration.ofMinutes(5);

    @Min(1)
    @Max(100)
    private int maxDevicesPerUser = 10;

    @NotBlank
    private String defaultNickname = "我的设备";
}

// 使用
@Service
@RequiredArgsConstructor
public class DeviceService {
    private final DeviceProperties deviceProperties;

    public boolean isOnline(Instant lastHeartbeat) {
        return Duration.between(lastHeartbeat, Instant.now())
                .compareTo(deviceProperties.getHeartbeatTimeout()) < 0;
    }
}
```

```yaml
# application.yml
app:
  device:
    heartbeat-timeout: 5m
    max-devices-per-user: 10
    default-nickname: 我的设备
```

### SPRING-010: 禁止硬编码配置

```java
// ❌ 错误: 硬编码配置
@Service
public class JwtService {
    private static final String SECRET = "my-secret-key";
    private static final long EXPIRATION = 3600000;
}

// ✅ 正确: 外部化配置
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    public String createToken(String userId) {
        return Jwts.builder()
                .setSubject(userId)
                .setExpiration(Date.from(
                    Instant.now().plus(jwtProperties.getExpiration())))
                .signWith(jwtProperties.getSecretKey())
                .compact();
    }
}
```

## 6. 事务管理

### SPRING-011: @Transactional 使用规范

```java
@Service
@RequiredArgsConstructor
public class DeviceService {

    // ✅ 正确: 写操作使用事务
    @Transactional
    public Device bindDevice(CreateDeviceRequest request, String userId) {
        // 多个写操作在同一事务中
        Device device = Device.create(request.getSerialNumber(), userId);
        deviceRepository.save(device);
        bindingRepository.save(new Binding(device.getId(), userId));
        return device;
    }

    // ✅ 正确: 只读查询使用 readOnly
    @Transactional(readOnly = true)
    public Optional<Device> findById(String id) {
        return deviceRepository.findById(id);
    }

    // ✅ 正确: 指定回滚异常
    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(String id, UpdateDeviceRequest request) {
        // ...
    }
}
```

### SPRING-012: 避免事务失效

```java
// ❌ 错误: 同类方法调用导致事务失效
@Service
public class DeviceService {

    @Transactional
    public void process() {
        // ...
    }

    public void doSomething() {
        this.process();  // 事务不生效!
    }
}

// ✅ 正确: 通过代理调用或拆分服务
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceTransactionService transactionService;

    public void doSomething() {
        transactionService.process();  // 通过另一个 Bean 调用
    }
}
```

## 7. 日志规范

### SPRING-013: 使用 Slf4j

```java
@Slf4j
@Service
public class DeviceService {

    public Device bindDevice(CreateDeviceRequest request, String userId) {
        log.info("Binding device: serialNumber={}, userId={}",
                 request.getSerialNumber(), userId);

        try {
            Device device = createDevice(request);
            log.debug("Device created: {}", device);
            return device;
        } catch (Exception e) {
            log.error("Failed to bind device: serialNumber={}",
                     request.getSerialNumber(), e);
            throw e;
        }
    }
}
```

## 8. 测试规范

### SPRING-014: 单元测试

```java
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void bindDevice_shouldCreateNewDevice() {
        // Given
        var request = new CreateDeviceRequest("SN1234567890", "MyDevice");
        when(deviceRepository.existsBySerialNumber(anyString())).thenReturn(false);

        // When
        Device result = deviceService.bindDevice(request, "user123");

        // Then
        assertThat(result.getSerialNumber()).isEqualTo("SN1234567890");
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void bindDevice_whenAlreadyBound_shouldThrowException() {
        // Given
        var request = new CreateDeviceRequest("SN1234567890", null);
        when(deviceRepository.existsBySerialNumber("SN1234567890")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> deviceService.bindDevice(request, "user123"))
                .isInstanceOf(DeviceAlreadyBoundException.class);
    }
}
```

### SPRING-015: 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "testuser")
    void bindDevice_shouldReturn201() throws Exception {
        var request = new CreateDeviceRequest("SN1234567890", "MyDevice");

        mockMvc.perform(post("/api/v1/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value("SN1234567890"));
    }
}
```
