package com.taskflow.api.dto.request;

import com.taskflow.api.domain.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 4000) String description,
        TaskPriority priority,
        UUID assigneeId,
        LocalDate dueDate) {
}
