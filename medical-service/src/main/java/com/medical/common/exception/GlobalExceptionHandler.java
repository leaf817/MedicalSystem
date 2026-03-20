package com.medical.common.exception;

import com.medical.common.response.GlobalCodeEnum;
import com.medical.common.response.ResultVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResultVo<?> handleServiceException(ServiceException e) {
        log.error("业务层异常: {}", e.getMessage());
        return ResultVo.build(GlobalCodeEnum.SERVICE_ERROR, e.getMessage());
    }

    @ExceptionHandler(BusinessWarningException.class)
    public ResultVo<?> handleBusinessWarningException(BusinessWarningException e) {
        log.info("业务提示: {}", e.getMessage());
        return ResultVo.bizWarning(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVo<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<String> errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(s -> s.getField() + ": " + s.getDefaultMessage())
                .collect(Collectors.toList());
        return ResultVo.build(GlobalCodeEnum.FAIL_9997, errorMessage);
    }

    @ExceptionHandler(BindException.class)
    public ResultVo<?> handleBindException(BindException e) {
        log.error("参数校验异常: {}", e.getMessage());
        List<String> errorMessage = e.getBindingResult().getFieldErrors().stream()
                .map(s -> s.getField() + ": " + s.getDefaultMessage())
                .collect(Collectors.toList());
        return ResultVo.build(GlobalCodeEnum.FAIL_9997, errorMessage);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResultVo<?> handleMethodNotSupportException(HttpRequestMethodNotSupportedException e) {
        String errorMessage = "此接口不支持 " + e.getMethod();
        return ResultVo.build(GlobalCodeEnum.FAIL_9996, errorMessage);
    }

    @ExceptionHandler(Exception.class)
    public ResultVo<?> handleDefaultException(Exception e) {
        log.error("系统错误: {}", e.getMessage(), e);
        return ResultVo.build(GlobalCodeEnum.INTERNAL_SERVER_ERROR, e.getMessage());
    }
}
