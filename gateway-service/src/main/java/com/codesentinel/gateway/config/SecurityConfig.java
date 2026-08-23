package com.codesentinel.gateway.config;

import com.codesentinel.common.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public JwtUtils jwtUtils() {
        return new JwtUtils(jwtSecret);
    }
}