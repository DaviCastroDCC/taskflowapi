package com.taskflow.api.dto.request;

import com.taskflow.api.domain.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeTaskStatusRequest(@NotNull TaskStatus status) {
}
