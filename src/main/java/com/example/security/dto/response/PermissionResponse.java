package com.example.security.dto.response;

import com.example.security.entity.Permission;
import lombok.Builder;
import lombok.Data;

@Data @Builder
public class PermissionResponse {
    private String name;
    private String authority;
    private String description;
    private String category;

    public static PermissionResponse from(Permission p) {
        return PermissionResponse.builder()
                .name(p.name())
                .authority(p.getAuthority())
                .description(p.getDescription())
                .category(p.getCategory())
                .build();
    }
}
