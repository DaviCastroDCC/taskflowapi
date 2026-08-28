package com.taskflow.api.service;

import com.taskflow.api.domain.Comment;
import com.taskflow.api.domain.Task;
import com.taskflow.api.domain.User;
import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.dto.request.CreateCommentRequest;
import com.taskflow.api.dto.response.CommentResponse;
import com.taskflow.api.exception.ForbiddenOperationException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.CommentRepository;
import com.taskflow.api.repository.TaskRepository;
import com.taskflow.api.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;

    public CommentService(
            CommentRepository commentRepository,
            TaskRepository taskRepository,
            UserRepository userRepository,
            WorkspaceAccess access) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional
    public CommentResponse add(UUID callerId, UUID taskId, CreateCommentRequest request) {
        Task task = loadTask(taskId);
        access.require(workspaceIdOf(task), callerId, WorkspaceRole.MEMBER);

        User author = userRepository
                .findById(callerId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", callerId));

        Comment comment = Comment.builder()
                .task(task)
                .author(author)
                .body(request.body().strip())
                .build();
        commentRepository.save(comment);
        return CommentResponse.of(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> list(UUID callerId, UUID taskId) {
        Task task = loadTask(taskId);
        access.require(workspaceIdOf(task), callerId, WorkspaceRole.VIEWER);
        return commentRepository.findAllForTask(taskId).stream()
                .map(CommentResponse::of)
                .toList();
    }

    @Transactional
    public void delete(UUID callerId, UUID commentId) {
        Comment comment = commentRepository
                .findById(commentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Comment", commentId));

        WorkspaceMember membership = access.require(workspaceIdOf(comment.getTask()), callerId, WorkspaceRole.VIEWER);
        boolean isAuthor = comment.getAuthor().getId().equals(callerId);
        if (!isAuthor && !membership.getRole().atLeast(WorkspaceRole.ADMIN)) {
            throw new ForbiddenOperationException("Only the comment's author or a workspace admin can delete it");
        }
        commentRepository.delete(comment);
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
