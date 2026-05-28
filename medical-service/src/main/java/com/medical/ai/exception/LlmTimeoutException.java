package com.medical.ai.exception;

import com.medical.common.exception.ServiceException;

/**
 * 大模型 API 调用超时
 */
public class LlmTimeoutException extends ServiceException {

    public LlmTimeoutException(String message) {
        super(message);
    }

    public LlmTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
