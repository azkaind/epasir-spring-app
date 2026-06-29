package com.example.security.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.UUID;

@Data
public class AssignRoleRequest {
    @NotNull(message = "Role ID tidak boleh kosong")
    private UUID roleId;
}
