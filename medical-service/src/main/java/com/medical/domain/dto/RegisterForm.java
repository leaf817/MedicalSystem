package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册表单（自助注册默认为患者角色）
 */
@Data
public class RegisterForm {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为 3～32 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6～64 个字符")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @Size(max = 50, message = "姓名最多 50 个字符")
    private String name;

    @Size(max = 20, message = "手机号过长")
    private String mobilePhone;
}
