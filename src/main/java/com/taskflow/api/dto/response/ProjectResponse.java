package com.taskflow.api.dto.response;

import com.taskflow.api.domain.Project;
import java.time.Instant;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String name,
        String key,
        String description,
        boolean archived,
        Instant createdAt) {

    public static ProjectResponse of(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.isArchived(),
                project.getCreatedAt());
    }
}
