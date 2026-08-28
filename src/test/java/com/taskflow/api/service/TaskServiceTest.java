package com.taskflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.taskflow.api.domain.Project;
import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.TaskStatus;
import com.taskflow.api.domain.User;
import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.ChangeTaskStatusRequest;
import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.response.TaskResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.exception.InvalidTaskTransitionException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.ProjectRepository;
import com.taskflow.api.repository.TaskRepository;
import com.taskflow.api.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAccess access;

    @InjectMocks
    private TaskService taskService;

    private final UUID callerId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private Project project;
    private User caller;

    @BeforeEach
    void setUp() {
        Workspace workspace = Workspace.builder().name("Acme").slug("acme").build();
        ReflectionTestUtils.setField(workspace, "id", workspaceId);

        project = Project.builder()
                .workspace(workspace)
                .name("Platform")
                .key("PLat".toUpperCase())
                .archived(false)
                .taskSequence(0L)
                .build();
        ReflectionTestUtils.setField(project, "id", projectId);

        caller = User.builder().email("caller@acme.test").passwordHash("x").fullName("Caller").enabled(true).build();
        ReflectionTestUtils.setField(caller, "id", callerId);
    }

    @Test
    void createAssignsTheNextProjectTaskNumberFromTheLockedRow() {
        when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
        when(access.require(eq(workspaceId), eq(callerId), eq(WorkspaceRole.MEMBER)))
                .thenReturn(member(WorkspaceRole.MEMBER));
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskResponse first = taskService.create(callerId, projectId, newTaskRequest("First"));

        assertThat(first.number()).isEqualTo(1);
        assertThat(first.status()).isEqualTo(TaskStatus.BACKLOG);
        assertThat(project.getTaskSequence()).isEqualTo(1);
    }

    @Test
    void createRejectsTasksOnAnArchivedProject() {
        project.setArchived(true);
        when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
        when(access.require(any(), any(), any())).thenReturn(member(WorkspaceRole.MEMBER));

        assertThatThrownBy(() -> taskService.create(callerId, projectId, newTaskRequest("Nope")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("archived");
    }

    @Test
    void createRejectsAnAssigneeWhoIsNotAWorkspaceMember() {
        UUID strangerId = UUID.randomUUID();
        when(projectRepository.findByIdForUpdate(projectId)).thenReturn(Optional.of(project));
        when(access.require(eq(workspaceId), eq(callerId), eq(WorkspaceRole.MEMBER)))
                .thenReturn(member(WorkspaceRole.MEMBER));
        lenient().when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(access.require(eq(workspaceId), eq(strangerId), eq(WorkspaceRole.VIEWER)))
                .thenThrow(ResourceNotFoundException.of("Workspace", workspaceId));

        CreateTaskRequest request =
                new CreateTaskRequest("Assigned", null, null, strangerId, null);

        assertThatThrownBy(() -> taskService.create(callerId, projectId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void changeStatusRejectsATransitionThatIsNotOnTheWorkflow() {
        Task task = existingTask(TaskStatus.BACKLOG);
        when(taskRepository.findDetailedById(task.getId())).thenReturn(Optional.of(task));
        when(access.require(any(), any(), any())).thenReturn(member(WorkspaceRole.MEMBER));

        assertThatThrownBy(() -> taskService.changeStatus(callerId, task.getId(),
                        new ChangeTaskStatusRequest(TaskStatus.DONE).status()))
                .isInstanceOf(InvalidTaskTransitionException.class);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.BACKLOG);
    }

    @Test
    void changeStatusAppliesAValidTransition() {
        Task task = existingTask(TaskStatus.BACKLOG);
        when(taskRepository.findDetailedById(task.getId())).thenReturn(Optional.of(task));
        when(access.require(any(), any(), any())).thenReturn(member(WorkspaceRole.MEMBER));

        TaskResponse updated = taskService.changeStatus(callerId, task.getId(), TaskStatus.TODO);

        assertThat(updated.status()).isEqualTo(TaskStatus.TODO);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    private CreateTaskRequest newTaskRequest(String title) {
        return new CreateTaskRequest(title, null, null, null, null);
    }

    private Task existingTask(TaskStatus status) {
        Task task = Task.builder()
                .project(project)
                .taskNumber(7)
                .title("Existing")
                .status(status)
                .reporter(caller)
                .build();
        ReflectionTestUtils.setField(task, "id", UUID.randomUUID());
        return task;
    }

    private WorkspaceMember member(WorkspaceRole role) {
        return WorkspaceMember.builder().role(role).build();
    }
}
