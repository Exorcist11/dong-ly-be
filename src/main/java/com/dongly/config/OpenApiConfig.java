package com.dongly.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình tài liệu hóa API tự động với OpenAPI 3 (SpringDoc).
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dong Ly Backend API")
                        .description("Tài liệu đặc tả các giao diện lập trình ứng dụng (RESTful API) nền tảng Đông Ly.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Dong Ly Engineering Team")
                                .email("contact@dongly.vn"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://dongly.vn")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập Access Token JWT dạng: Bearer {token}")));
    }
}
