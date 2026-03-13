package com.specflow.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 Controller - 用于验证 API、日志、traceId 功能
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "健康检查接口")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping("/hello")
    @Operation(summary = "Hello 接口", description = "测试接口，验证日志和 traceId 功能")
    public Map<String, Object> hello() {
        log.debug("Debug log test");
        log.info("Hello endpoint called");
        log.warn("Warning log test");

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello from SpecFlow API!");
        response.put("timestamp", Instant.now().toString());
        response.put("version", "0.1.0-SNAPSHOT");

        return response;
    }
}
