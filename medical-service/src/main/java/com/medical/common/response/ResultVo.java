package com.medical.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 统一封装后台响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultVo<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> ResultVo<T> ok() {
        return new ResultVo<>(GlobalCodeEnum.SUCCESS_000.getCode(), GlobalCodeEnum.SUCCESS_000.getDescription(), null);
    }

    public static <T> ResultVo<T> ok(T data) {
        return new ResultVo<>(GlobalCodeEnum.SUCCESS_000.getCode(), GlobalCodeEnum.SUCCESS_000.getDescription(), data);
    }

    public static <T> ResultVo<T> fail() {
        return new ResultVo<>(GlobalCodeEnum.INTERNAL_SERVER_ERROR.getCode(), GlobalCodeEnum.INTERNAL_SERVER_ERROR.getDescription(), null);
    }

    public static <T> ResultVo<T> build(GlobalCodeEnum globalCodeEnum, T data) {
        return new ResultVo<>(globalCodeEnum.getCode(), globalCodeEnum.getDescription(), data);
    }

    public static <T> ResultVo<T> build(GlobalCodeEnum globalCodeEnum) {
        return build(globalCodeEnum, null);
    }

    public static ResultVo<Void> bizWarning(String message) {
        ResultVo<Void> vo = new ResultVo<>();
        vo.setCode(GlobalCodeEnum.BIZ_WARNING.getCode());
        vo.setMessage(message);
        vo.setData(null);
        return vo;
    }
}
