package com.dongly.common.api;

import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;

/**
 * Cấu trúc phản hồi dữ liệu danh sách phân trang chuẩn.
 *
 * @param <T> kiểu dữ liệu phần tử trong danh sách
 */
public record PageResponse<T>(
        boolean success,
        String message,
        PageData<T> data,
        Instant timestamp
) {

    public record PageData<T>(
            List<T> items,
            PaginationMeta pagination
    ) {}

    public record PaginationMeta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean isFirst,
            boolean isLast
    ) {}

    public static <T> PageResponse<T> of(Page<T> springPage, String message) {
        PaginationMeta meta = new PaginationMeta(
                springPage.getNumber(),
                springPage.getSize(),
                springPage.getTotalElements(),
                springPage.getTotalPages(),
                springPage.isFirst(),
                springPage.isLast()
        );
        PageData<T> pageData = new PageData<>(springPage.getContent(), meta);
        return new PageResponse<>(true, message, pageData, Instant.now());
    }

    public static <T> PageResponse<T> of(Page<T> springPage) {
        return of(springPage, "Lấy danh sách thành công");
    }
}
