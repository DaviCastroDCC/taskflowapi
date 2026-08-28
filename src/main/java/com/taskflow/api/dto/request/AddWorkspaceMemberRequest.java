package com.taskflow.api.dto.request;

import com.taskflow.api.domain.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddWorkspaceMemberRequest(@NotBlank @Email String email, @NotNull WorkspaceRole role) {
}
