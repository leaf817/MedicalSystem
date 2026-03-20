package com.medical.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录用户信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVo {
    private Long userId;
    private String username;
    private String name;
    private String avatarUrl;
    private List<String> roles;
}
