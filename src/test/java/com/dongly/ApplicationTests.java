package com.dongly;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    @DisplayName("Khởi tạo ApplicationContext thành công với cấu hình test")
    void contextLoads() {
        assertThat(true).isTrue();
    }
}
