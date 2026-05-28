package com.medical.ai.exception;

import com.medical.common.exception.ServiceException;

/**
 * 大模型 API 调用失败（鉴权、限流、业务错误等）
 */
public class LlmApiException extends ServiceException {

    public LlmApiException(String message) {
        super(message);
    }

    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
