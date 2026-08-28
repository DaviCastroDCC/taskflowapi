package com.taskflow.api.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Workflow states a task moves through. Transitions are restricted to the edges declared
 * in {@link #ALLOWED_TRANSITIONS} so a task can't jump e.g. straight from BACKLOG to DONE.
 */
public enum TaskStatus {
    BACKLOG,
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(BACKLOG, EnumSet.of(TODO));
        ALLOWED_TRANSITIONS.put(TODO, EnumSet.of(BACKLOG, IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(IN_PROGRESS, EnumSet.of(TODO, IN_REVIEW));
        ALLOWED_TRANSITIONS.put(IN_REVIEW, EnumSet.of(IN_PROGRESS, DONE));
        ALLOWED_TRANSITIONS.put(DONE, EnumSet.of(IN_REVIEW));
    }

    public boolean canTransitionTo(TaskStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }
}
