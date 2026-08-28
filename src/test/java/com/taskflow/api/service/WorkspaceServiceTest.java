package com.taskflow.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.taskflow.api.domain.User;
import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.AddWorkspaceMemberRequest;
import com.taskflow.api.dto.request.CreateWorkspaceRequest;
import com.taskflow.api.dto.request.UpdateMemberRoleRequest;
import com.taskflow.api.dto.response.WorkspaceResponse;
import com.taskflow.api.exception.ConflictException;
import com.taskflow.api.exception.ForbiddenOperationException;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.repository.WorkspaceMemberRepository;
import com.taskflow.api.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceAccess access;

    @InjectMocks
    private WorkspaceService workspaceService;

    private final UUID callerId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();
    private User caller;

    @BeforeEach
    void setUp() {
        caller = User.builder().email("owner@acme.test").passwordHash("x").fullName("Owner").enabled(true).build();
        ReflectionTestUtils.setField(caller, "id", callerId);
    }

    @Test
    void createMakesTheCallerTheOwnerMember() {
        when(userRepository.findById(callerId)).thenReturn(Optional.of(caller));
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        WorkspaceResponse response = workspaceService.create(callerId, new CreateWorkspaceRequest("Acme Rockets"));

        assertThat(response.callerRole()).isEqualTo(WorkspaceRole.OWNER);
        assertThat(response.slug()).isEqualTo("acme-rockets");

        ArgumentCaptor<Workspace> saved = ArgumentCaptor.forClass(Workspace.class);
        org.mockito.Mockito.verify(workspaceRepository).save(saved.capture());
        assertThat(saved.getValue().getMembers())
                .singleElement()
                .satisfies(member -> {
                    assertThat(member.getRole()).isEqualTo(WorkspaceRole.OWNER);
                    assertThat(member.getUser()).isSameAs(caller);
                });
    }

    @Test
    void addMemberRefusesToGrantTheOwnerRole() {
        when(access.require(workspaceId, callerId, WorkspaceRole.ADMIN)).thenReturn(member(WorkspaceRole.ADMIN));

        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest("someone@acme.test", WorkspaceRole.OWNER);

        assertThatThrownBy(() -> workspaceService.addMember(callerId, workspaceId, request))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void addMemberRejectsSomeoneAlreadyInTheWorkspace() {
        UUID targetId = UUID.randomUUID();
        User target = User.builder().email("dupe@acme.test").passwordHash("x").fullName("Dupe").enabled(true).build();
        ReflectionTestUtils.setField(target, "id", targetId);

        when(access.require(workspaceId, callerId, WorkspaceRole.ADMIN)).thenReturn(member(WorkspaceRole.ADMIN));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(new Workspace()));
        when(userRepository.findByEmailIgnoreCase("dupe@acme.test")).thenReturn(Optional.of(target));
        when(memberRepository.existsByWorkspaceIdAndUserId(workspaceId, targetId)).thenReturn(true);

        AddWorkspaceMemberRequest request = new AddWorkspaceMemberRequest("dupe@acme.test", WorkspaceRole.MEMBER);

        assertThatThrownBy(() -> workspaceService.addMember(callerId, workspaceId, request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void changeMemberRoleCannotTouchTheOwner() {
        UUID ownerUserId = UUID.randomUUID();
        when(access.require(workspaceId, callerId, WorkspaceRole.ADMIN)).thenReturn(member(WorkspaceRole.ADMIN));
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerUserId))
                .thenReturn(Optional.of(member(WorkspaceRole.OWNER)));

        assertThatThrownBy(() -> workspaceService.changeMemberRole(
                        callerId, workspaceId, ownerUserId, new UpdateMemberRoleRequest(WorkspaceRole.MEMBER)))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private WorkspaceMember member(WorkspaceRole role) {
        return WorkspaceMember.builder().role(role).build();
    }
}
