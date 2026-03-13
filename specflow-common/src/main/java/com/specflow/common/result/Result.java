package com.specflow.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应对象
 *
 * @param <T> 数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {

    /**
     * 请求是否成功
     */
    private Boolean success;

    /**
     * 业务错误码（成功时为 null）
     */
    private String code;

    /**
     * 提示信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    // ==================== 成功响应工厂方法 ====================

    /**
     * 成功响应（无数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(true, null, "success", null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, null, "success", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, null, message, data);
    }

    /**
     * 成功响应（带业务码、消息和数据）
     */
    public static <T> Result<T> success(String code, String message, T data) {
        return new Result<>(true, code, message, data);
    }

    // ==================== 失败响应工厂方法 ====================

    /**
     * 失败响应（仅消息）
     */
    public static <T> Result<T> failure(String message) {
        return new Result<>(false, "FAILURE", message, null);
    }

    /**
     * 失败响应（带错误码和消息）
     */
    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(false, code, message, null);
    }

    /**
     * 失败响应（带错误码、消息和数据）
     */
    public static <T> Result<T> failure(String code, String message, T data) {
        return new Result<>(false, code, message, data);
    }
}
