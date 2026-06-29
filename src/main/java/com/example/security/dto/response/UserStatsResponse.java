package com.example.security.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data @Builder
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long disabledUsers;
    private Map<String, Long> usersByRole;
}
