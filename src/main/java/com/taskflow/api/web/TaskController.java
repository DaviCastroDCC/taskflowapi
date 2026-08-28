package com.taskflow.api.web;

import com.taskflow.api.domain.TaskPriority;
import com.taskflow.api.domain.TaskStatus;
import com.taskflow.api.dto.request.ChangeTaskStatusRequest;
import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.request.UpdateTaskRequest;
import com.taskflow.api.dto.response.PageResponse;
import com.taskflow.api.dto.response.TaskResponse;
import com.taskflow.api.dto.response.TaskSummaryResponse;
import com.taskflow.api.security.UserPrincipal;
import com.taskflow.api.service.TaskFilter;
import com.taskflow.api.service.TaskService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/api/v1/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(caller.getId(), projectId, request);
    }

    @GetMapping("/api/v1/projects/{projectId}/tasks")
    public PageResponse<TaskSummaryResponse> list(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskPriority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        TaskFilter filter = new TaskFilter(status, priority, assigneeId, search);
        return taskService.list(caller.getId(), projectId, filter, pageable);
    }

    @GetMapping("/api/v1/tasks/{taskId}")
    public TaskResponse get(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID taskId) {
        return taskService.get(caller.getId(), taskId);
    }

    @PutMapping("/api/v1/tasks/{taskId}")
    public TaskResponse update(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(caller.getId(), taskId, request);
    }

    @PatchMapping("/api/v1/tasks/{taskId}/status")
    public TaskResponse changeStatus(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID taskId,
            @Valid @RequestBody ChangeTaskStatusRequest request) {
        return taskService.changeStatus(caller.getId(), taskId, request.status());
    }

    @DeleteMapping("/api/v1/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID taskId) {
        taskService.delete(caller.getId(), taskId);
    }
}
