package com.taskflow.api.repository.spec;

import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class TaskSpecifications {

    private TaskSpecifications() {
    }

    public static Specification<Task> belongsToProject(UUID projectId) {
        return (root, query, cb) -> cb.equal(root.get("project").get("id"), projectId);
    }

    public static Specification<Task> hasStatus(TaskStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Task> hasPriority(TaskPriority priority) {
        return priority == null ? null : (root, query, cb) -> cb.equal(root.get("priority"), priority);
    }

    public static Specification<Task> assignedTo(UUID assigneeId) {
        return assigneeId == null ? null : (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    public static Specification<Task> titleContains(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String pattern = "%" + text.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")), pattern);
    }
}
