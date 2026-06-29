package com.example.security.dto.response;

import com.example.security.entity.AppRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data @Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleResponse {
    private UUID    id;
    private String  name;
    private String  description;
    private String  color;
    private boolean systemRole;
    private long    userCount;
    private Set<PermissionResponse> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RoleResponse from(AppRole role, long userCount) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .color(role.getColor())
                .systemRole(role.isSystemRole())
                .userCount(userCount)
                .permissions(role.getPermissions().stream()
                        .map(PermissionResponse::from)
                        .collect(Collectors.toSet()))
                .createdAt(role.getCreatedAt())
                .updatedAt(role.getUpdatedAt())
                .build();
    }
}
