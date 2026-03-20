package com.medical.common.response;

import lombok.Getter;

/**
 * 全局响应码枚举
 */
@Getter
public enum GlobalCodeEnum {
    SUCCESS_000(200, "success"),
    FAIL_9996(996, "HTTP请求方法错误"),
    FAIL_9997(997, "参数校验错误"),
    FAIL_9998(998, "参数类型转换错误"),
    INTERNAL_SERVER_ERROR(500, "服务器内部异常"),
    FORBIDDEN(403, "没有相应权限，无法访问"),
    UNAUTHORIZED(401, "登录已过期或未登录"),
    SERVICE_ERROR(503, "系统服务层异常"),
    LOGIN_ERROR(1199, "用户名或密码错误"),
    USER_DISABLED(1299, "用户已被禁用"),
    AUTH_ERROR(1399, "认证失败"),
    DATA_DUPLICATE(1999, "数据重复"),
    BIZ_WARNING(400, "业务提示");

    private final Integer code;
    private final String description;

    GlobalCodeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }
}
