package com.medical.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ReceptionPatientRegisterDto {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度为 3～32 个字符")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度为 6～64 个字符")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名最多 50 个字符")
    private String name;

    @Size(max = 20, message = "手机号过长")
    private String mobilePhone;

    @Size(max = 18, message = "身份证号过长")
    private String idCard;

    private String gender;

    private LocalDate birthDate;

    @Size(max = 200, message = "地址过长")
    private String address;
}
