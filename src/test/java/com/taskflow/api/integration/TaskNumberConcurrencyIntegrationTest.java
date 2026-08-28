package com.taskflow.api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.taskflow.api.domain.User;
import com.taskflow.api.dto.request.CreateProjectRequest;
import com.taskflow.api.dto.request.CreateTaskRequest;
import com.taskflow.api.dto.request.CreateWorkspaceRequest;
import com.taskflow.api.repository.ProjectRepository;
import com.taskflow.api.repository.UserRepository;
import com.taskflow.api.service.ProjectService;
import com.taskflow.api.service.TaskService;
import com.taskflow.api.service.WorkspaceService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Fires a batch of task creations at one project in parallel and checks that the per-project
 * numbering stays gap-free and collision-free. This is the payoff of locking the project row
 * in {@code TaskService.create}: without the lock, threads read the same counter value and
 * either clash on the unique constraint or skip numbers.
 */
class TaskNumberConcurrencyIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final int CONCURRENT_CREATIONS = 24;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProjectRepository projectRepository;

    @Test
    void parallelTaskCreationProducesContiguousUniqueNumbers() throws Exception {
        UUID authorId = newUser();
        UUID workspaceId = workspaceService
                .create(authorId, new CreateWorkspaceRequest("Race Conditions Inc"))
                .id();
        UUID projectId = projectService
                .create(authorId, workspaceId, new CreateProjectRequest("Core", "CORE", null))
                .id();

        List<Long> assignedNumbers = createTasksConcurrently(authorId, projectId);

        assertThat(assignedNumbers).hasSize(CONCURRENT_CREATIONS);
        assertThat(assignedNumbers).doesNotHaveDuplicates();
        assertThat(assignedNumbers)
                .containsExactlyInAnyOrderElementsOf(
                        LongStream.rangeClosed(1, CONCURRENT_CREATIONS).boxed().toList());

        long persistedSequence =
                projectRepository.findById(projectId).orElseThrow().getTaskSequence();
        assertThat(persistedSequence).isEqualTo(CONCURRENT_CREATIONS);
    }

    private List<Long> createTasksConcurrently(UUID authorId, UUID projectId) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(CONCURRENT_CREATIONS, 12));
        CountDownLatch startGate = new CountDownLatch(1);
        try {
            List<Future<Long>> futures = IntStream.rangeClosed(1, CONCURRENT_CREATIONS)
                    .mapToObj(index -> pool.submit(() -> {
                        startGate.await();
                        return taskService
                                .create(authorId, projectId, new CreateTaskRequest("Task " + index, null, null, null, null))
                                .number();
                    }))
                    .toList();

            startGate.countDown();

            List<Long> numbers = new ArrayList<>(CONCURRENT_CREATIONS);
            for (Future<Long> future : futures) {
                numbers.add(future.get());
            }
            return numbers;
        } finally {
            pool.shutdownNow();
        }
    }

    private UUID newUser() {
        return userRepository
                .save(User.builder()
                        .email("author-" + UUID.randomUUID() + "@race.test")
                        .passwordHash("irrelevant")
                        .fullName("Concurrent Author")
                        .enabled(true)
                        .build())
                .getId();
    }
}
