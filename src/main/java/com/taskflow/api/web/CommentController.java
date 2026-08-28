package com.taskflow.api.web;

import com.taskflow.api.dto.request.CreateCommentRequest;
import com.taskflow.api.dto.response.CommentResponse;
import com.taskflow.api.security.UserPrincipal;
import com.taskflow.api.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/api/v1/tasks/{taskId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse add(
            @AuthenticationPrincipal UserPrincipal caller,
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateCommentRequest request) {
        return commentService.add(caller.getId(), taskId, request);
    }

    @GetMapping("/api/v1/tasks/{taskId}/comments")
    public List<CommentResponse> list(
            @AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID taskId) {
        return commentService.list(caller.getId(), taskId);
    }

    @DeleteMapping("/api/v1/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal caller, @PathVariable UUID commentId) {
        commentService.delete(caller.getId(), commentId);
    }
}
