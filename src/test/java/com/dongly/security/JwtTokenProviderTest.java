package com.dongly.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        // Khóa bí mật tối thiểu 256 bits (32 bytes) cho kiểm thử
        String testSecret = "01234567890123456789012345678901234567890123456789";
        JwtProperties properties = new JwtProperties(testSecret, 3600, 86400);
        jwtTokenProvider = new JwtTokenProvider(properties);
    }

    @Test
    @DisplayName("Tạo Access Token và xác thực thành công")
    void generateAccessToken_withValidData_returnsValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "testuser@dongly.vn";
        List<String> roles = List.of("ROLE_USER", "ROLE_ADMIN");

        String token = jwtTokenProvider.generateAccessToken(userId, email, roles);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        CurrentUser currentUser = jwtTokenProvider.extractCurrentUser(token);
        assertThat(currentUser.id()).isEqualTo(userId);
        assertThat(currentUser.email()).isEqualTo(email);
        assertThat(currentUser.roles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
        assertThat(currentUser.isAdmin()).isTrue();
    }

    @Test
    @DisplayName("Tạo Refresh Token thành công")
    void generateRefreshToken_withValidData_returnsValidToken() {
        UUID userId = UUID.randomUUID();
        String email = "testuser@dongly.vn";

        String refreshToken = jwtTokenProvider.generateRefreshToken(userId, email);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(refreshToken)).isTrue();
    }

    @Test
    @DisplayName("Token không hợp lệ trả về false")
    void validateToken_withInvalidToken_returnsFalse() {
        String invalidToken = "invalid.bearer.token";
        assertThat(jwtTokenProvider.validateToken(invalidToken)).isFalse();
    }

    @Test
    @DisplayName("Token đã hết hạn trả về false")
    void validateToken_withExpiredToken_returnsFalse() {
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                "01234567890123456789012345678901234567890123456789".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String expiredToken = io.jsonwebtoken.Jwts.builder()
                .subject("expired@dongly.vn")
                .claim("uid", UUID.randomUUID().toString())
                .issuedAt(java.util.Date.from(java.time.Instant.now().minus(2, java.time.temporal.ChronoUnit.HOURS)))
                .expiration(java.util.Date.from(java.time.Instant.now().minus(1, java.time.temporal.ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }
}
