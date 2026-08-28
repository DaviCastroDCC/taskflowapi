package com.taskflow.api.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WorkspaceRoleTest {

    @Test
    void higherRolesSatisfyLowerRequirements() {
        assertThat(WorkspaceRole.OWNER.atLeast(WorkspaceRole.ADMIN)).isTrue();
        assertThat(WorkspaceRole.ADMIN.atLeast(WorkspaceRole.MEMBER)).isTrue();
        assertThat(WorkspaceRole.MEMBER.atLeast(WorkspaceRole.VIEWER)).isTrue();
    }

    @Test
    void aRoleSatisfiesItsOwnRequirement() {
        assertThat(WorkspaceRole.MEMBER.atLeast(WorkspaceRole.MEMBER)).isTrue();
    }

    @Test
    void lowerRolesDoNotSatisfyHigherRequirements() {
        assertThat(WorkspaceRole.VIEWER.atLeast(WorkspaceRole.MEMBER)).isFalse();
        assertThat(WorkspaceRole.MEMBER.atLeast(WorkspaceRole.ADMIN)).isFalse();
        assertThat(WorkspaceRole.ADMIN.atLeast(WorkspaceRole.OWNER)).isFalse();
    }
}
