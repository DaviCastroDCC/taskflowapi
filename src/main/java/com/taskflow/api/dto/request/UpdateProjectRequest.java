package com.taskflow.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @NotBlank @Size(max = 120) String name, @Size(max = 1000) String description, boolean archived) {
}
