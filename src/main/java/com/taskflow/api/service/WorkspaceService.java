package com.taskflow.api.service;

import com.taskflow.api.common.Slugs;
import com.taskflow.api.domain.User;
import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.AddWorkspaceMemberRequest;
import com.taskflow.api.dto.request.CreateWorkspaceRequest;
import com.taskflow.api.dto.request.UpdateMemberRoleRequest;
import com.taskflow.api.dto.response.WorkspaceMemberResponse;
import com.taskflow.api.dto.response.WorkspaceResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.exception.ForbiddenOperationException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.repository.WorkspaceMemberRepository;
import com.taskflow.api.repository.WorkspaceRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceAccess access) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional
    public WorkspaceResponse create(UUID callerId, CreateWorkspaceRequest request) {
        User owner = userRepository
                .findById(callerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", callerId));

        Workspace workspace = Workspace.builder()
                .name(request.name().strip())
                .slug(uniqueSlug(request.name()))
                .owner(owner)
                .build();

        workspace.getMembers()
                .add(WorkspaceMember.builder()
                        .workspace(workspace)
                        .user(owner)
                        .role(WorkspaceRole.OWNER)
                        .build());

        workspaceRepository.save(workspace); // cascades to the owner membership
        return WorkspaceResponse.of(workspace, WorkspaceRole.OWNER);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForCaller(UUID callerId) {
        return memberRepository.findAllForUser(callerId).stream()
                .map(member -> WorkspaceResponse.of(member.getWorkspace(), member.getRole()))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID callerId, UUID workspaceId) {
        WorkspaceMember membership = access.require(workspaceId, callerId, WorkspaceRole.VIEWER);
        return WorkspaceResponse.of(membership.getWorkspace(), membership.getRole());
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> listMembers(UUID callerId, UUID workspaceId) {
        access.require(workspaceId, callerId, WorkspaceRole.VIEWER);
        return memberRepository.findAllByWorkspace(workspaceId).stream()
                .map(WorkspaceMemberResponse::of)
                .toList();
    }

    @Transactional
    public WorkspaceMemberResponse addMember(UUID callerId, UUID workspaceId, AddWorkspaceMemberRequest request) {
        access.require(workspaceId, callerId, WorkspaceRole.ADMIN);
        rejectOwnerRole(request.role());

        Workspace workspace = workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Workspace", workspaceId));
        User user = userRepository
                .findByEmailIgnoreCase(request.email().strip().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email " + request.email()));

        if (memberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
            throw new ConflictException("That user is already a member of this workspace");
        }

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(user)
                .role(request.role())
                .build();
        memberRepository.save(member);
        return WorkspaceMemberResponse.of(member);
    }

    @Transactional
    public WorkspaceMemberResponse changeMemberRole(
            UUID callerId, UUID workspaceId, UUID memberUserId, UpdateMemberRoleRequest request) {
        access.require(workspaceId, callerId, WorkspaceRole.ADMIN);
        rejectOwnerRole(request.role());

        WorkspaceMember member = requireMember(workspaceId, memberUserId);
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException("The workspace owner's role can't be changed here");
        }
        member.setRole(request.role());
        return WorkspaceMemberResponse.of(member);
    }

    @Transactional
    public void removeMember(UUID callerId, UUID workspaceId, UUID memberUserId) {
        access.require(workspaceId, callerId, WorkspaceRole.ADMIN);

        WorkspaceMember member = requireMember(workspaceId, memberUserId);
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException("The workspace owner can't be removed");
        }
        memberRepository.delete(member);
    }

    private WorkspaceMember requireMember(UUID workspaceId, UUID memberUserId) {
        return memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, memberUserId)
                .orElseThrow(() -> ResourceNotFoundException.of("Workspace member", memberUserId));
    }

    private static void rejectOwnerRole(WorkspaceRole role) {
        if (role == WorkspaceRole.OWNER) {
            throw new ForbiddenOperationException("A workspace has exactly one owner; use ADMIN instead");
        }
    }

    private String uniqueSlug(String name) {
        String base = Slugs.slugify(name);
        String candidate = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
