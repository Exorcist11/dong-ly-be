package com.dongly.modules.auth.dto;

/**
 * Phản hồi xác thực chỉ chứa token tương ứng theo yêu cầu bảo mật.
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse of(String accessToken, String refreshToken, long expiresIn) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresIn);
    }
}
