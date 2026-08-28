package com.taskflow.api.dto.response;

import com.taskflow.api.domain.User;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, Instant createdAt) {

    public static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
