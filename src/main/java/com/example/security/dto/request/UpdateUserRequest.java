package com.example.security.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.UUID;

@Data
public class UpdateUserRequest {
    @Size(max = 100)
    private String fullName;

    @Email @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phoneNumber;

    @Size(min = 8, max = 100)
    private String newPassword;

    private UUID roleId;
    private Boolean enabled;
}
