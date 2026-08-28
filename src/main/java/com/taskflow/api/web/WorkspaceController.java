package com.taskflow.api.web;

import com.taskflow.api.dto.request.AddWorkspaceMemberRequest;
import com.taskflow.api.dto.request.CreateWorkspaceRequest;
import com.taskflow.api.dto.request.UpdateMemberRoleRequest;
import com.taskflow.api.dto.response.WorkspaceMemberResponse;
import com.taskflow.api.dto.response.WorkspaceResponse;
import com.taskflow.api.security.UserPrincipal;
import com.taskflow.api.service.WorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceResponse create(
            @AuthenticationPrincipal UserPrincipal caller, @Valid @RequestBody CreateWorkspaceRequest request) {
        return workspaceService.create(caller.getId(), request);
    }

    @GetMapping
    public List<WorkspaceResponse> listMine(@AuthenticationPrincipal UserPrincipal caller) {
        return workspaceService.listForCaller(caller.getId());
    }

    @GetMapping("/{workspaceId}")
    public WorkspaceResponse get(
            @AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID workspaceId) {
        return workspaceService.get(caller.getId(), workspaceId);
    }

    @GetMapping("/{workspaceId}/members")
    public List<WorkspaceMemberResponse> listMembers(
            @AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID workspaceId) {
        return workspaceService.listMembers(caller.getId(), workspaceId);
    }

    @PostMapping("/{workspaceId}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceMemberResponse addMember(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddWorkspaceMemberRequest request) {
        return workspaceService.addMember(caller.getId(), workspaceId, request);
    }

    @PatchMapping("/{workspaceId}/members/{userId}")
    public WorkspaceMemberResponse changeMemberRole(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return workspaceService.changeMemberRole(caller.getId(), workspaceId, userId, request);
    }

    @DeleteMapping("/{workspaceId}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId) {
        workspaceService.removeMember(caller.getId(), workspaceId, userId);
    }
}
