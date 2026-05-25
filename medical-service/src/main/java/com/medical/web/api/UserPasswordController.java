package com.medical.web.api;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.ChangePasswordDto;
import com.medical.domain.entity.SysUser;
import com.medical.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 全角色 — 修改当前登录用户密码
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserPasswordController {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @PutMapping("/password")
    @Transactional(rollbackFor = Exception.class)
    public ResultVo<Void> changePassword(@Valid @RequestBody ChangePasswordDto dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessWarningException("两次输入的新密码不一致");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessWarningException("新密码不能与原密码相同");
        }

        SysUser user = requireCurrentUser();
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessWarningException("原密码不正确");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdatedTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        return ResultVo.ok();
    }

    private SysUser requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new BusinessWarningException("请先登录");
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName()));
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessWarningException("账号已禁用，无法修改密码");
        }
        return user;
    }
}
