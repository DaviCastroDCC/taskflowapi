package com.taskflow.api.exception;

import com.taskflow.api.domain.TaskStatus;

public class InvalidTaskTransitionException extends RuntimeException {

    public InvalidTaskTransitionException(TaskStatus from, TaskStatus to) {
        super("Cannot move a task from %s to %s".formatted(from, to));
    }
}
