package com.taskflow.api.web;

import com.taskflow.api.dto.request.CreateProjectRequest;
import com.taskflow.api.dto.request.UpdateProjectRequest;
import com.taskflow.api.dto.response.ProjectResponse;
import com.taskflow.api.security.UserPrincipal;
import com.taskflow.api.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping("/api/v1/workspaces/{workspaceId}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateProjectRequest request) {
        return projectService.create(caller.getId(), workspaceId, request);
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/projects")
    public List<ProjectResponse> list(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID workspaceId,
            @RequestParam(defaultValue = "false") boolean includeArchived) {
        return projectService.list(caller.getId(), workspaceId, includeArchived);
    }

    @GetMapping("/api/v1/projects/{projectId}")
    public ProjectResponse get(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID projectId) {
        return projectService.get(caller.getId(), projectId);
    }

    @PutMapping("/api/v1/projects/{projectId}")
    public ProjectResponse update(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(caller.getId(), projectId, request);
    }

    @DeleteMapping("/api/v1/projects/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID projectId) {
        projectService.delete(caller.getId(), projectId);
    }
}
