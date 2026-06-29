package com.example.security.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username tidak boleh kosong")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username hanya huruf, angka, underscore")
    private String username;

    @NotBlank(message = "Email tidak boleh kosong")
    @Email(message = "Format email tidak valid")
    @Size(max = 100)
    private String email;

    @NotBlank(message = "Password tidak boleh kosong")
    @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$",
             message = "Password harus mengandung huruf besar, kecil, angka, dan karakter spesial")
    private String password;

    @NotBlank(message = "Nama lengkap tidak boleh kosong")
    @Size(max = 100)
    private String fullName;

    @Size(max = 20)
    private String phoneNumber;

    @NotNull(message = "Role ID tidak boleh kosong")
    private UUID roleId;
}
