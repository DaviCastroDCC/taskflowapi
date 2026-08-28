package com.taskflow.api.dto.response;

import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        String code,
        UUID projectId,
        long number,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        UserSummary assignee,
        UserSummary reporter,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse of(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getCode(),
                task.getProject().getId(),
                task.getTaskNumber(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                UserSummary.of(task.getAssignee()),
                UserSummary.of(task.getReporter()),
                task.getDueDate(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
