package com.medical.common.exception;

/**
 * 业务提示异常（前端展示为 warning 而非 error）
 */
public class BusinessWarningException extends RuntimeException {

    public BusinessWarningException(String message) {
        super(message);
    }
}
