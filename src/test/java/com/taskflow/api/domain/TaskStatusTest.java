package com.taskflow.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TaskStatusTest {

    @Test
    void backlogOnlyMovesToTodo() {
        assertThat(TaskStatus.BACKLOG.canTransitionTo(TaskStatus.TODO)).isTrue();
        assertThat(TaskStatus.BACKLOG.canTransitionTo(TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatus.BACKLOG.canTransitionTo(TaskStatus.DONE)).isFalse();
    }

    @Test
    void progressIsReversibleOneStepAtATime() {
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.IN_REVIEW)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.TODO)).isTrue();
        assertThat(TaskStatus.IN_REVIEW.canTransitionTo(TaskStatus.DONE)).isTrue();
        assertThat(TaskStatus.DONE.canTransitionTo(TaskStatus.IN_REVIEW)).isTrue();
    }

    @Test
    void doneCannotJumpBackToProgress() {
        assertThat(TaskStatus.DONE.canTransitionTo(TaskStatus.IN_PROGRESS)).isFalse();
        assertThat(TaskStatus.DONE.canTransitionTo(TaskStatus.TODO)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void aStatusIsNeverAllowedToTransitionToItself(TaskStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void everyStatusHasAtLeastOneWayOut(TaskStatus status) {
        Set<TaskStatus> reachable = EnumSet.noneOf(TaskStatus.class);
        for (TaskStatus target : TaskStatus.values()) {
            if (status.canTransitionTo(target)) {
                reachable.add(target);
            }
        }
        assertThat(reachable).isNotEmpty();
    }
}
