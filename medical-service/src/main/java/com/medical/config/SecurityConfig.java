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
                        // 白名单接口（无需认证）
                        .requestMatchers(WHITE_LIST).permitAll()

                        // ========== 角色管理（仅超级管理员） ==========
                        .requestMatchers(HttpMethod.GET, "/api/admin/role/list").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/role/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/manage/**").hasRole("SUPER_ADMIN")

                        // ========== 科室接口（多端共用） ==========
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/options").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/tree").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/page").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT", "DOCTOR")

                        // ========== 用户/医生/药品查询（多端共用） ==========
                        .requestMatchers(HttpMethod.GET, "/api/admin/user/page").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/admin/user/getPatientId/**").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/medicine/page").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/medicine/categories").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")

                        // ========== 排班接口 ==========
                        .requestMatchers(HttpMethod.GET, "/api/admin/schedule/list").hasAnyRole("ADMIN", "SUPER_ADMIN", "DOCTOR")

                        // ========== 其他 admin 接口（增删改操作） ==========
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // ========== 各端接口 ==========
                        .requestMatchers("/api/doctor/**").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/reception/**").hasAnyRole("RECEPTIONIST", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/nurse/**").hasAnyRole("NURSE", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/patient/**").hasAnyRole("PATIENT", "DOCTOR")

                        // 其他所有请求需要认证
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