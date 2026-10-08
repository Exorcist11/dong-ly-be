package com.dongly.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptHashTest {

    @Test
    @DisplayName("BCrypt cost factor 12 mã hóa và khớp mật khẩu chính xác")
    void passwordHashing_withCost12_encodesAndMatches() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String rawPassword = "Password@123";

        String hash = encoder.encode(rawPassword);

        assertThat(hash).isNotNull().startsWith("$2a$12$");
        assertThat(encoder.matches(rawPassword, hash)).isTrue();
        assertThat(encoder.matches("WrongPassword", hash)).isFalse();
    }
}
