package com.dongly.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestExceptionController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException trả về HTTP 404 và mã RESOURCE_NOT_FOUND")
    void handleResourceNotFoundException_returns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Không tìm thấy tài nguyên kiểm thử")))
                .andExpect(jsonPath("$.path", is("/test/not-found")));
    }

    @Test
    @DisplayName("Ném BusinessRuleException trả về HTTP 422 và mã BUSINESS_RULE_VIOLATION")
    void handleBusinessRuleException_returns422() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is(422)))
                .andExpect(jsonPath("$.code", is("BUSINESS_RULE_VIOLATION")))
                .andExpect(jsonPath("$.message", is("Vi phạm quy tắc kiểm thử")));
    }

    @Test
    @DisplayName("Lỗi xác thực Bean Validation trả về HTTP 400 kèm mảng errors chi tiết")
    void handleMethodArgumentNotValid_returns400WithErrors() throws Exception {
        String invalidJson = "{\"name\": \"\"}";

        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].field", is("name")));
    }

    @Test
    @DisplayName("Ngoại lệ chưa xử lý trả về HTTP 500 với thông điệp an toàn")
    void handleUnhandledException_returns500WithSafeMessage() throws Exception {
        mockMvc.perform(get("/test/server-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.message", is("Đã xảy ra lỗi hệ thống nội bộ. Vui lòng thử lại sau.")));
    }

    // Controller nội bộ phục vụ việc kiểm thử ngoại lệ
    @RestController
    @RequestMapping("/test")
    static class TestExceptionController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Không tìm thấy tài nguyên kiểm thử");
        }

        @GetMapping("/business-error")
        public void throwBusinessRule() {
            throw new BusinessRuleException("Vi phạm quy tắc kiểm thử");
        }

        @GetMapping("/server-error")
        public void throwUnexpected() {
            throw new RuntimeException("Lỗi database mô phỏng - không được lộ");
        }

        @PostMapping("/validate")
        public void validateDto(@Valid @RequestBody TestRequest request) {
            // No-op
        }
    }

    record TestRequest(@NotBlank(message = "Tên không được để trống") String name) {}
}
