package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long   expiresIn;
    private String username;
    private String email;
    private String role;       // Sekarang String bukan enum
    private String message;

    public static AuthResponse of(String accessToken, String refreshToken,
                                   long expiresIn, String username,
                                   String email, String role) {
        return AuthResponse.builder()
                .accessToken(accessToken).refreshToken(refreshToken)
                .tokenType("Bearer").expiresIn(expiresIn)
                .username(username).email(email).role(role)
                .build();
    }
}
