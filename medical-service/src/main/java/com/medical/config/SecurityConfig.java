package com.medical.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] WHITE_LIST = {
            "/api/login",
            "/api/register",
            "/api/logout",
            "/api/captcha",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-resources/**",
            "/favicon.ico"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST).permitAll()
                        // 角色下拉列表供医院管理员维护用户；其余角色 CRUD 仅超级管理员
                        .requestMatchers(HttpMethod.GET, "/api/admin/role/list").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/role/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/manage/**").hasRole("SUPER_ADMIN")
                        // 科室接口允许患者访问
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/options").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/tree").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/page").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        // 用户/医生查询接口允许患者访问（用于预约时查看医生列表）
                        .requestMatchers(HttpMethod.GET, "/api/admin/user/page").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        // 其他 admin 接口保持原样
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        // 医生端接口权限（包括取消预约）
                        .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/reception/**").hasAnyRole("RECEPTIONIST", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/nurse/**").hasAnyRole("NURSE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
                        .anyRequest().authenticated()
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}