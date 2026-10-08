package com.dongly.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình tham số cho JWT từ file application.yml.
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationSeconds,
        long refreshTokenExpirationSeconds
) {
    public JwtProperties {
        if (accessTokenExpirationSeconds <= 0) {
            accessTokenExpirationSeconds = 1800; // 30 phút mặc định
        }
        if (refreshTokenExpirationSeconds <= 0) {
            refreshTokenExpirationSeconds = 604800; // 7 ngày mặc định
        }
    }
}
