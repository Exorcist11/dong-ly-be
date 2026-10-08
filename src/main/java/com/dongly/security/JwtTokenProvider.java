package com.dongly.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Thành phần xử lý việc tạo, giải mã và xác thực JSON Web Token (JWT).
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = initSecretKey(jwtProperties.secret());
    }

    private SecretKey initSecretKey(String secret) {
        if (secret == null || secret.trim().length() < 32) {
            log.warn("JWT secret key chưa được cấu hình hoặc ngắn hơn 32 ký tự. Đang sử dụng khóa tạm thời cho môi trường phát triển.");
            // Tạo secret key có độ dài tối thiểu 256-bit chuẩn HMAC-SHA cho môi trường dev/test
            String fallback = "dong-ly-backend-fallback-secret-key-for-development-must-be-at-least-256-bits";
            return Keys.hmacShaKeyFor(fallback.getBytes(StandardCharsets.UTF_8));
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Tạo Access Token cho người dùng đã xác thực.
     */
    public String generateAccessToken(UUID userId, String email, Collection<String> roles) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.accessTokenExpirationSeconds(), ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Tạo Refresh Token với thời gian sống dài hơn.
     */
    public String generateRefreshToken(UUID userId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.refreshTokenExpirationSeconds(), ChronoUnit.SECONDS);

        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Trích xuất CurrentUser từ JWT token hợp lệ.
     */
    @SuppressWarnings("unchecked")
    public CurrentUser extractCurrentUser(String token) {
        Claims claims = parseClaims(token);
        UUID userId = UUID.fromString(claims.get(CLAIM_USER_ID, String.class));
        String email = claims.getSubject();
        List<String> rawRoles = claims.get(CLAIM_ROLES, List.class);
        Set<String> roles = (rawRoles != null) ? new HashSet<>(rawRoles) : Set.of();

        return new CurrentUser(userId, email, roles);
    }

    /**
     * Kiểm tra tính hợp lệ của token JWT.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Chữ ký JWT không hợp lệ: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Mã thông báo JWT đã hết hạn: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Mã thông báo JWT không được hỗ trợ: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Chuỗi claims JWT trống: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("Lỗi phân tích JWT: {}", e.getMessage());
        }
        return false;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
