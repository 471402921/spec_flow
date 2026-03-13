# 安全检查清单（Java / Spring Boot）

## 1. 敏感信息处理

### SEC-001: 禁止硬编码敏感信息

```java
// ❌ 错误
public class JwtConfig {
    private static final String SECRET = "my-super-secret-key";
    private static final String DB_PASSWORD = "admin123";
    private static final String API_KEY = "sk-1234567890";
}

// ✅ 正确: 使用配置属性
@Configuration
@ConfigurationProperties(prefix = "app.jwt")
@Data
public class JwtProperties {
    private String secret;
    private Duration expiration;
}

// 或使用 @Value 注入
@Value("${app.jwt.secret}")
private String jwtSecret;
```

### SEC-002: 检测模式

```
敏感关键词: password, secret, key, token, credential, api_key, apikey
```

```java
// 检测硬编码的敏感信息正则
// (password|secret|key|token|credential|api[_-]?key)\s*=\s*["'][^"']{8,}["']
```

### SEC-003: 配置文件安全

```yaml
# ❌ 错误: application.yml 中硬编码
spring:
  datasource:
    password: admin123

# ✅ 正确: 使用环境变量
spring:
  datasource:
    password: ${DB_PASSWORD}

# ✅ 正确: 使用 Spring Cloud Config / Vault
spring:
  cloud:
    config:
      uri: http://config-server:8888
```

## 2. 输入验证

### SEC-004: 所有用户输入必须验证

```java
// ✅ 正确: 使用 Jakarta Validation
public class CreateDeviceRequest {

    @NotBlank(message = "序列号不能为空")
    @Size(min = 10, max = 20, message = "序列号长度必须在10-20之间")
    @Pattern(regexp = "^[A-Z0-9]+$", message = "序列号只能包含大写字母和数字")
    private String serialNumber;

    @Size(max = 50, message = "昵称长度不能超过50")
    private String nickname;
}

// Controller 中使用 @Valid
@PostMapping
public DeviceResponse createDevice(@Valid @RequestBody CreateDeviceRequest request) {
    // ...
}
```

### SEC-005: 验证必须覆盖

- 请求 Body（@RequestBody + @Valid）
- 请求 Query 参数（@RequestParam + @Validated）
- 请求 Path 参数（@PathVariable + 业务验证）
- WebSocket 消息（手动验证）
- 文件上传（类型、大小、内容验证）

### SEC-006: 防止路径遍历

```java
// ❌ 危险: 直接拼接用户输入
public void downloadFile(String filename) {
    Path filePath = Paths.get("/uploads/" + filename);
    // 攻击者可能输入 "../../../etc/passwd"
}

// ✅ 安全: 验证并规范化路径
public void downloadFile(String filename) {
    // 仅保留文件名，去除路径
    String sanitized = Paths.get(filename).getFileName().toString();

    Path basePath = Paths.get("/uploads").toAbsolutePath().normalize();
    Path filePath = basePath.resolve(sanitized).normalize();

    // 确保最终路径仍在基础目录内
    if (!filePath.startsWith(basePath)) {
        throw new SecurityException("Invalid file path");
    }
}
```

## 3. 认证授权

### SEC-007: 敏感接口必须鉴权

```java
// ✅ 正确: 使用 Spring Security
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/v1/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt())
            .build();
    }
}
```

### SEC-008: 使用 @PreAuthorize 细粒度控制

```java
@RestController
@RequestMapping("/api/v1/devices")
public class DeviceController {

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/my-devices")
    public List<DeviceResponse> getMyDevices(@AuthenticationPrincipal UserDetails user) {
        return deviceService.findByOwner(user.getUsername());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void deleteDevice(@PathVariable String id) {
        deviceService.delete(id);
    }

    // 方法级权限检查
    @PreAuthorize("#deviceId == authentication.principal.id or hasRole('ADMIN')")
    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(@PathVariable String deviceId) {
        // ...
    }
}
```

### SEC-009: 资源访问权限检查

```java
// ✅ 正确: 检查资源所有权
@Service
@RequiredArgsConstructor
public class DeviceService {

    public Device getDevice(String userId, String deviceId) {
        Device device = deviceRepository.findById(deviceId)
            .orElseThrow(() -> new DeviceNotFoundException(deviceId));

        // 检查所有权
        if (!device.getOwnerId().equals(userId)) {
            throw new AccessDeniedException("无权访问此设备");
        }

        return device;
    }
}
```

## 4. 数据库安全

### SEC-010: 防止 SQL 注入（MyBatis）

```java
// ❌ 危险: 使用 ${} 直接拼接
// Mapper.xml
<select id="findByName" resultType="Device">
    SELECT * FROM device WHERE name = '${name}'  <!-- 危险! -->
</select>

// ✅ 安全: 使用 #{} 参数化
<select id="findByName" resultType="Device">
    SELECT * FROM device WHERE name = #{name}
</select>

// ✅ 安全: 使用 MyBatis-Plus Wrapper
LambdaQueryWrapper<Device> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(Device::getName, name);
deviceMapper.selectOne(wrapper);
```

### SEC-011: 动态 SQL 安全

```xml
<!-- ❌ 危险: 动态列名/表名使用 ${} -->
<select id="findByColumn" resultType="Device">
    SELECT * FROM device WHERE ${column} = #{value}
</select>

<!-- ✅ 安全: 白名单校验 -->
```

```java
// Java 代码中校验
private static final Set<String> ALLOWED_COLUMNS = Set.of("name", "status", "type");

public List<Device> findByColumn(String column, String value) {
    if (!ALLOWED_COLUMNS.contains(column)) {
        throw new IllegalArgumentException("Invalid column: " + column);
    }
    return deviceMapper.findByColumn(column, value);
}
```

## 5. 日志安全

### SEC-012: 禁止记录敏感信息

```java
// ❌ 错误
log.info("User login: username={}, password={}", username, password);
log.debug("API response: {}", response);  // 可能包含敏感数据

// ✅ 正确
log.info("User {} logged in successfully", username);
log.debug("API request completed for user {}", userId);
```

### SEC-013: 日志脱敏

```java
// 脱敏工具类
public class LogSanitizer {

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
        "password", "token", "secret", "apiKey", "creditCard"
    );

    public static String sanitize(Object obj) {
        if (obj == null) return "null";

        String json = objectMapper.writeValueAsString(obj);
        // 替换敏感字段值为 ***
        for (String field : SENSITIVE_FIELDS) {
            json = json.replaceAll(
                "\"" + field + "\"\\s*:\\s*\"[^\"]+\"",
                "\"" + field + "\":\"***\""
            );
        }
        return json;
    }
}
```

## 6. 错误处理

### SEC-014: 不要暴露内部错误

```java
// ❌ 错误: 暴露内部信息
@ExceptionHandler(Exception.class)
public ResponseEntity<String> handleException(Exception e) {
    return ResponseEntity.status(500)
        .body("Error: " + e.getMessage() + "\nStack: " + Arrays.toString(e.getStackTrace()));
}

// ✅ 正确: 返回友好错误，内部记录详情
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
    log.error("Unexpected error", e);  // 内部日志记录完整信息
    return ResponseEntity.status(500)
        .body(ApiResponse.error(500, "服务暂时不可用，请稍后重试"));
}
```

## 7. CORS 和请求头

### SEC-015: 正确配置 CORS

```java
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 正确: 限制来源
        configuration.setAllowedOrigins(List.of(
            "https://app.specflow.me",
            "https://admin.specflow.me"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}

// ❌ 危险: 允许所有来源
configuration.setAllowedOrigins(List.of("*"));
configuration.setAllowCredentials(true);  // 与 * 冲突，会报错
```

## 8. 速率限制

### SEC-016: 防止暴力破解

```java
// 使用 bucket4j 或自定义限流
@Configuration
public class RateLimitConfig {

    @Bean
    public Bucket loginRateLimiter() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
            .build();
    }
}

// 在 Controller 中使用
@PostMapping("/login")
public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    if (!loginRateLimiter.tryConsume(1)) {
        throw new TooManyRequestsException("请求过于频繁，请稍后重试");
    }
    return authService.login(request);
}
```

### SEC-017: 使用 Spring Cloud Gateway 限流

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: api_route
          uri: http://localhost:8080
          predicates:
            - Path=/api/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10
                redis-rate-limiter.burstCapacity: 20
```

## 9. 密码安全

### SEC-018: 使用 BCrypt 加密密码

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

// 使用
@Service
@RequiredArgsConstructor
public class UserService {

    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // 存储加密后的密码
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
```

## 10. JWT 安全

### SEC-019: JWT 最佳实践

```java
// ✅ 正确: 使用强密钥和合理过期时间
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;

    public String createToken(String userId) {
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(Instant.now().plus(jwtProperties.getExpiration())))
            .signWith(getSecretKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private SecretKey getSecretKey() {
        // 密钥至少 256 位
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
```

## 检查优先级

| 优先级 | 检查项 | 说明 |
|--------|--------|------|
| P0 | SEC-001 | 硬编码敏感信息 |
| P0 | SEC-010 | SQL 注入 |
| P0 | SEC-007 | 缺少认证 |
| P1 | SEC-004 | 输入验证不完整 |
| P1 | SEC-009 | 资源权限检查 |
| P1 | SEC-018 | 密码存储 |
| P2 | SEC-012 | 日志脱敏 |
| P2 | SEC-015 | CORS 配置 |
| P2 | SEC-016 | 速率限制 |
