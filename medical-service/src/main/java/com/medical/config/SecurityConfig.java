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
                        .requestMatchers(WHITE_LIST).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/admin/role/list").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/role/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/manage/**").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/options").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/tree").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/admin/dept/page").hasAnyRole("ADMIN", "SUPER_ADMIN", "PATIENT")

                        // ========== 医生端和患者端需要调用的接口（必须放在 /api/admin/** 之前） ==========
                        .requestMatchers(HttpMethod.GET, "/api/admin/user/page").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN", "PATIENT")
                        .requestMatchers(HttpMethod.GET, "/api/admin/medicine/page").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/user/getPatientId/**").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/medicine/categories").hasAnyRole("DOCTOR", "ADMIN", "SUPER_ADMIN")

                        // ========== 其他 admin 接口 ==========
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // ========== 各端接口 ==========
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