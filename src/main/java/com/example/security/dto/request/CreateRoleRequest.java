package com.example.security.dto.request;

import com.example.security.entity.Permission;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class CreateRoleRequest {
    @NotBlank(message = "Nama role tidak boleh kosong")
    @Pattern(regexp = "^ROLE_[A-Z_]+$",
             message = "Nama role harus diawali 'ROLE_' dan hanya huruf kapital dan underscore")
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Format warna harus hex (mis. #1A237E)")
    private String color = "#1A237E";

    private Set<Permission> permissions;
}
