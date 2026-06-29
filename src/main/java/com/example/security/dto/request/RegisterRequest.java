package com.example.security.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO untuk request registrasi user baru.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "Username tidak boleh kosong")
    @Size(min = 3, max = 50, message = "Username harus 3-50 karakter")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
             message = "Username hanya boleh mengandung huruf, angka, dan underscore")
    private String username;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, max = 100, message = "Password minimal 8 karakter")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Password harus mengandung huruf besar, huruf kecil, angka, dan karakter spesial")
    private String password;

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    @Size(max = 100)
    private String fullName;
}
