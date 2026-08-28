package com.taskflow.api.dto.response;

import com.taskflow.api.domain.Comment;
import java.time.Instant;
import java.util.UUID;

public record CommentResponse(UUID id, UUID taskId, UserSummary author, String body, Instant createdAt) {

    public static CommentResponse of(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                UserSummary.of(comment.getAuthor()),
                comment.getBody(),
                comment.getCreatedAt());
    }
}
