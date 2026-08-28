package com.taskflow.api.service;

import com.taskflow.api.common.Text;
import com.taskflow.api.domain.Project;
import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import com.taskflow.api.domain.User;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.request.UpdateTaskRequest;
import com.taskflow.api.dto.response.PageResponse;
import com.taskflow.api.dto.response.TaskResponse;
import com.taskflow.api.dto.response.TaskSummaryResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.exception.InvalidTaskTransitionException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.ProjectRepository;
import com.taskflow.api.repository.TaskRepository;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.repository.spec.TaskSpecifications;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;

    public TaskService(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            UserRepository userRepository,
            WorkspaceAccess access) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional
    public TaskResponse create(UUID callerId, UUID projectId, CreateTaskRequest request) {
        // Lock the project row for the transaction: nextTaskNumber() reads and increments a
        // per-project counter, and two concurrent creates must not be handed the same number.
        Project project = projectRepository
                .findByIdForUpdate(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));

        UUID workspaceId = project.getWorkspace().getId();
        access.require(workspaceId, callerId, WorkspaceRole.MEMBER);
        if (project.isArchived()) {
            throw new ConflictException("Can't add tasks to an archived project");
        }

        User reporter = userRepository
                .findById(callerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", callerId));

        Task task = Task.builder()
                .project(project)
                .taskNumber(project.nextTaskNumber())
                .title(request.title().strip())
                .description(Text.trimToNull(request.description()))
                .priority(request.priority() == null ? TaskPriority.MEDIUM : request.priority())
                .status(TaskStatus.BACKLOG)
                .reporter(reporter)
                .assignee(resolveAssignee(workspaceId, request.assigneeId()))
                .dueDate(request.dueDate())
                .build();
        taskRepository.save(task);
        return TaskResponse.of(task);
    }

    @Transactional(readOnly = true)
    public PageResponse<TaskSummaryResponse> list(UUID callerId, UUID projectId, TaskFilter filter, Pageable pageable) {
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> ResourceNotFoundException.of("Project", projectId));
        access.require(project.getWorkspace().getId(), callerId, WorkspaceRole.VIEWER);

        Specification<Task> spec = Specification.allOf(
                TaskSpecifications.belongsToProject(projectId),
                TaskSpecifications.hasStatus(filter.status()),
                TaskSpecifications.hasPriority(filter.priority()),
                TaskSpecifications.assignedTo(filter.assigneeId()),
                TaskSpecifications.titleContains(filter.search()));

        return PageResponse.of(taskRepository.findAll(spec, pageable), TaskSummaryResponse::of);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID callerId, UUID taskId) {
        Task task = loadTask(taskId);
        access.require(workspaceIdOf(task), callerId, WorkspaceRole.VIEWER);
        return TaskResponse.of(task);
    }

    @Transactional
    public TaskResponse update(UUID callerId, UUID taskId, UpdateTaskRequest request) {
        Task task = loadTask(taskId);
        UUID workspaceId = workspaceIdOf(task);
        access.require(workspaceId, callerId, WorkspaceRole.MEMBER);

        task.setTitle(request.title().strip());
        task.setDescription(Text.trimToNull(request.description()));
        if (request.priority() != null) {
            task.setPriority(request.priority());
        }
        task.setAssignee(resolveAssignee(workspaceId, request.assigneeId()));
        task.setDueDate(request.dueDate());
        return TaskResponse.of(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID callerId, UUID taskId, TaskStatus target) {
        Task task = loadTask(taskId);
        access.require(workspaceIdOf(task), callerId, WorkspaceRole.MEMBER);

        TaskStatus current = task.getStatus();
        if (current == target) {
            return TaskResponse.of(task);
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidTaskTransitionException(current, target);
        }
        task.setStatus(target);
        return TaskResponse.of(task);
    }

    @Transactional
    public void delete(UUID callerId, UUID taskId) {
        Task task = loadTask(taskId);
        access.require(workspaceIdOf(task), callerId, WorkspaceRole.MEMBER);
        taskRepository.delete(task);
    }

    private User resolveAssignee(UUID workspaceId, UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        // require(...) answers NOT_FOUND when the target isn't in the workspace, which is the
        // right response for "assignee must be a workspace member".
        access.require(workspaceId, assigneeId, WorkspaceRole.VIEWER);
        return userRepository
                .findById(assigneeId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", assigneeId));
    }

    private Task loadTask(UUID taskId) {
        return taskRepository
                .findDetailedById(taskId)
                .orElseThrow(() -> ResourceNotFoundException.of("Task", taskId));
    }

    private static UUID workspaceIdOf(Task task) {
        return task.getProject().getWorkspace().getId();
    }
}
