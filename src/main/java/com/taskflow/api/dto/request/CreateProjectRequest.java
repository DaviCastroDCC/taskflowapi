package com.taskflow.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^[A-Z][A-Z0-9]{1,9}$", message = "must be 2-10 uppercase letters/digits, starting with a letter")
                String key,
        @Size(max = 1000) String description) {
}
