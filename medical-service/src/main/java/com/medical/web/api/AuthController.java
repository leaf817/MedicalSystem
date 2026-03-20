package com.medical.web.api;

import com.medical.common.response.GlobalCodeEnum;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.LoginForm;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.UserInfoVo;
import com.medical.mapper.SysRoleMapper;
import com.medical.mapper.SysUserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证控制器
 */
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    @PostMapping("/api/login")
    public ResultVo<UserInfoVo> login(@Valid @RequestBody LoginForm form,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(form.getUsername(), form.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            HttpSessionSecurityContextRepository repository = new HttpSessionSecurityContextRepository();
            repository.saveContext(SecurityContextHolder.getContext(), request, response);

            SysUser user = sysUserMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                            .eq(SysUser::getUsername, form.getUsername())
            );
            List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getUserId());
            List<String> roleNames = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(s -> s.replace("ROLE_", ""))
                    .collect(Collectors.toList());

            UserInfoVo vo = UserInfoVo.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .name(user.getName())
                    .avatarUrl(user.getAvatarUrl())
                    .roles(roleNames)
                    .build();

            return ResultVo.ok(vo);
        } catch (BadCredentialsException e) {
            return ResultVo.build(GlobalCodeEnum.LOGIN_ERROR);
        } catch (DisabledException e) {
            return ResultVo.build(GlobalCodeEnum.USER_DISABLED);
        } catch (AuthenticationException e) {
            return ResultVo.build(GlobalCodeEnum.AUTH_ERROR);
        }
    }

    @PostMapping("/api/logout")
    public ResultVo<String> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }
        return ResultVo.ok("注销成功");
    }

    @GetMapping("/api/user/info")
    public ResultVo<UserInfoVo> getUserInfo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResultVo.build(GlobalCodeEnum.UNAUTHORIZED);
        }
        String username = auth.getName();
        SysUser user = sysUserMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
        );
        if (user == null) {
            return ResultVo.build(GlobalCodeEnum.UNAUTHORIZED);
        }
        List<String> roles = sysRoleMapper.selectRoleCodesByUserId(user.getUserId());
        UserInfoVo vo = UserInfoVo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .roles(roles)
                .build();
        return ResultVo.ok(vo);
    }
}
