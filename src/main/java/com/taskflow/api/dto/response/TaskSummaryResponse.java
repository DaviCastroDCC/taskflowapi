package com.taskflow.api.dto.response;

import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import java.time.LocalDate;
import java.util.UUID;

/** Trimmed task view used for list and board endpoints. */
public record TaskSummaryResponse(
        UUID id,
        String code,
        String title,
        TaskStatus status,
        TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate) {

    public static TaskSummaryResponse of(Task task) {
        return new TaskSummaryResponse(
                task.getId(),
                task.getCode(),
                task.getTitle(),
                task.getStatus(),
                task.getPriority(),
                task.getAssignee() == null ? null : task.getAssignee().getId(),
                task.getDueDate());
    }
}
