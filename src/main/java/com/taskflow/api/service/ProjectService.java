package com.taskflow.api.service;

import com.taskflow.api.common.Text;
import com.taskflow.api.domain.Project;
import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.CreateProjectRequest;
import com.taskflow.api.dto.request.UpdateProjectRequest;
import com.taskflow.api.dto.response.ProjectResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.ProjectRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceAccess access;

    public ProjectService(ProjectRepository projectRepository, WorkspaceAccess access) {
        this.projectRepository = projectRepository;
        this.access = access;
    }

    @Transactional
    public ProjectResponse create(UUID callerId, UUID workspaceId, CreateProjectRequest request) {
        Workspace workspace = access.requireWorkspace(workspaceId, callerId, WorkspaceRole.MEMBER);

        String key = request.key().toUpperCase(Locale.ROOT);
        if (projectRepository.existsByWorkspaceIdAndKeyIgnoreCase(workspaceId, key)) {
            throw new ConflictException("A project with key '%s' already exists in this workspace".formatted(key));
        }

        Project project = Project.builder()
                .workspace(workspace)
                .name(request.name().strip())
                .key(key)
                .description(Text.trimToNull(request.description()))
                .archived(false)
                .build();
        projectRepository.save(project);
        return ProjectResponse.of(project);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID callerId, UUID workspaceId, boolean includeArchived) {
        access.require(workspaceId, callerId, WorkspaceRole.VIEWER);
        return projectRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
                .filter(project -> includeArchived || !project.isArchived())
                .map(ProjectResponse::of)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse get(UUID callerId, UUID projectId) {
        Project project = loadProject(projectId);
        access.require(project.getWorkspace().getId(), callerId, WorkspaceRole.VIEWER);
        return ProjectResponse.of(project);
    }

    @Transactional
    public ProjectResponse update(UUID callerId, UUID projectId, UpdateProjectRequest request) {
        Project project = loadProject(projectId);
        access.require(project.getWorkspace().getId(), callerId, WorkspaceRole.MEMBER);

        project.setName(request.name().strip());
        project.setDescription(Text.trimToNull(request.description()));
        project.setArchived(request.archived());
        return ProjectResponse.of(project);
    }

    @Transactional
    public void delete(UUID callerId, UUID projectId) {
        Project project = loadProject(projectId);
        access.require(project.getWorkspace().getId(), callerId, WorkspaceRole.ADMIN);
        projectRepository.delete(project);
    }

    private Project loadProject(UUID projectId) {
        return projectRepository
                .findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
    }
}
