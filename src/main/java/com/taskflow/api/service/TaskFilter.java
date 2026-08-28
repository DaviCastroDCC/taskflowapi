package com.taskflow.api.service;

import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import java.util.UUID;

/** Optional filters for the task listing endpoint; any field may be {@code null}. */
public record TaskFilter(TaskStatus status, TaskPriority priority, UUID assigneeId, String search) {

    public static TaskFilter empty() {
        return new TaskFilter(null, null, null, null);
    }
}
