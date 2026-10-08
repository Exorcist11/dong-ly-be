package com.dongly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Kích hoạt cơ chế tự động ghi nhận thời gian (JPA Auditing) cho các thực thể cơ sở dữ liệu.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
