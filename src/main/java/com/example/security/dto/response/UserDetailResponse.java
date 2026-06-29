package com.example.security.dto.response;

import com.example.security.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetailResponse {
    private UUID     id;
    private String   username;
    private String   email;
    private String   fullName;
    private String   phoneNumber;
    private RoleResponse role;
    private boolean  enabled;
    private boolean  accountNonLocked;
    private int      failedLoginAttempts;
    private LocalDateTime lastLogin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDetailResponse from(User user, long userCount) {
        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole() != null
                        ? RoleResponse.from(user.getRole(), userCount) : null)
                .enabled(user.isEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .failedLoginAttempts(user.getFailedLoginAttempts())
                .lastLogin(user.getLastLogin())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
