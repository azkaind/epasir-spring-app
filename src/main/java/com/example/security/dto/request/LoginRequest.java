package com.example.security.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO untuk request login.
 */
@Data
public class LoginRequest {

    @NotBlank(message = "Username tidak boleh kosong")
    private String username;

    @NotBlank(message = "Password tidak boleh kosong")
    private String password;
}
