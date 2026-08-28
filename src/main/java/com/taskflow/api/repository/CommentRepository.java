package com.taskflow.api.repository;

import com.taskflow.api.domain.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query("select c from Comment c join fetch c.author where c.task.id = :taskId order by c.createdAt asc")
    List<Comment> findAllForTask(@Param("taskId") UUID taskId);
}
