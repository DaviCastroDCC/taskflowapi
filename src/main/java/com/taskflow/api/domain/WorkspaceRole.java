package com.taskflow.api.domain;

/**
 * Access level a user holds within a workspace, ordered from least to most privileged.
 */
public enum WorkspaceRole {
    VIEWER,
    MEMBER,
    ADMIN,
    OWNER;

    public boolean atLeast(WorkspaceRole required) {
        return this.ordinal() >= required.ordinal();
    }
}
