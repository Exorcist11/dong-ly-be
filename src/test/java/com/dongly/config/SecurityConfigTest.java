package com.dongly.config;

import com.dongly.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("Endpoint /actuator/health được mở công khai không cần token")
    void actuatorHealth_isPermittedWithoutAuth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Endpoint /api/v1/auth/login mở công khai, không bị chặn bởi JWT filter")
    void loginEndpoint_isPermittedWithoutAuth() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("Endpoint yêu cầu bảo mật trả về 401 khi không có Authorization header")
    void securedEndpoint_withoutToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Yêu cầu xác thực tài khoản")));
    }

    @Test
    @DisplayName("Endpoint yêu cầu bảo mật trả về 401 khi token không hợp lệ")
    void securedEndpoint_withInvalidToken_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer invalid-token-string"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Endpoint yêu cầu quyền hạn trả về 403 Forbidden khi thiếu permission")
    void securedEndpoint_withoutPermission_returns403Forbidden() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                "testuser",
                "test@dongly.vn",
                List.of("ROLE_STAFF"),
                List.of() // không có quyền USER_READ
        );

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Endpoint tiếp nhận request khi token có quyền USER_READ hợp lệ")
    void securedEndpoint_withPermission_passesSecurityFilter() throws Exception {
        String token = jwtTokenProvider.generateAccessToken(
                UUID.randomUUID(),
                "admin",
                "admin@dongly.vn",
                List.of("ROLE_ADMIN"),
                List.of("USER_READ")
        );

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
