package com.example.security.dto.request;

import com.example.security.entity.Permission;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateRoleRequest {
    @Size(max = 200)
    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    private String color;

    private Set<Permission> permissions;
}
