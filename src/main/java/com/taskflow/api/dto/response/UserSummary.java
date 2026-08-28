package com.taskflow.api.dto.response;

import com.taskflow.api.domain.User;
import java.util.UUID;

/** Minimal user projection embedded in task and comment payloads. */
public record UserSummary(UUID id, String fullName) {

    public static UserSummary of(User user) {
        return user == null ? null : new UserSummary(user.getId(), user.getFullName());
    }
}
