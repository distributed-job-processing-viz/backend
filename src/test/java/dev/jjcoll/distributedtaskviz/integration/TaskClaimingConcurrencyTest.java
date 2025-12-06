package dev.jjcoll.distributedtaskviz.integration;

import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.model.*;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.repository.WorkerRepository;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

@SpringBootTest
@ActiveProfiles("test")
class TaskClaimingConcurrencyTest {

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @AfterEach
    void cleanup() {
        // Clean up all tasks and workers from database after each test
        taskRepository.deleteAll();
        workerRepository.deleteAll();
    }

    @Test
    void testPessimisticLocking_MultipleConcurrentClaims_OnlyOneSucceeds() throws Exception {
        // Given: ONE PENDING task
        Task task = createTask("concurrent-task", Complexity.LOW);

        // Given: THREE workers (entities, not simulations)
        Worker worker1 = createWorkerEntity("worker-1");
        Worker worker2 = createWorkerEntity("worker-2");
        Worker worker3 = createWorkerEntity("worker-3");

        // When: Three threads try to claim the same task simultaneously
        CountDownLatch startLatch = new CountDownLatch(3);
        CountDownLatch doneLatch = new CountDownLatch(3);

        AtomicInteger successCount = new AtomicInteger(0);
        List<Optional<Task>> results = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(3);

        for (Worker worker : List.of(worker1, worker2, worker3)) {
            executor.submit(() -> {
                startLatch.countDown();
                try {
                    startLatch.await(); // Ensure all start at same time

                    Optional<Task> claimed = taskService.claimTaskForWorker(worker);

                    results.add(claimed);
                    if (claimed.isPresent()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Expected: some might get lock timeout or transaction issues
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Wait for all threads to complete (max 10 seconds)
        assertTimeout(Duration.ofSeconds(10), () -> {
            doneLatch.await();
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Exactly ONE worker succeeded
        assertThat(successCount.get()).isEqualTo(1);

        // Then: Task assigned to exactly one worker
        Task finalTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(finalTask.getAssignedWorker()).isNotNull();
        assertThat(finalTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void testHighConcurrency_100WorkersTrying10Tasks() throws Exception {
        // Given: 10 PENDING tasks
        List<Task> tasks = IntStream.range(0, 10)
                .mapToObj(i -> createTask("task-" + i, Complexity.LOW))
                .collect(Collectors.toList());

        // Given: 100 worker entities
        List<Worker> workers = IntStream.range(0, 100)
                .mapToObj(i -> createWorkerEntity("worker-" + i))
                .collect(Collectors.toList());

        // When: All workers try to claim tasks simultaneously
        CountDownLatch startLatch = new CountDownLatch(100);
        CountDownLatch doneLatch = new CountDownLatch(100);

        AtomicInteger totalClaims = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(100);

        for (Worker worker : workers) {
            executor.submit(() -> {
                startLatch.countDown();
                try {
                    startLatch.await();

                    Optional<Task> claimed = taskService.claimTaskForWorker(worker);

                    if (claimed.isPresent()) {
                        totalClaims.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Some might timeout or fail
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        assertTimeout(Duration.ofSeconds(30), () -> {
            doneLatch.await();
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Exactly 10 tasks claimed (one per task)
        assertThat(totalClaims.get()).isEqualTo(10);

        // Then: All tasks assigned to workers
        List<Task> claimedTasks = taskRepository.findAll();
        assertThat(claimedTasks)
                .filteredOn(t -> tasks.stream().anyMatch(original -> original.getId().equals(t.getId())))
                .allMatch(t -> t.getAssignedWorker() != null)
                .allMatch(t -> t.getStatus() == TaskStatus.PROCESSING);

        // Then: Each task assigned to exactly one worker
        Set<Long> assignedWorkerIds = claimedTasks.stream()
                .filter(t -> tasks.stream().anyMatch(original -> original.getId().equals(t.getId())))
                .map(t -> t.getAssignedWorker().getId())
                .collect(Collectors.toSet());
        assertThat(assignedWorkerIds).hasSize(10); // 10 unique workers
    }

    @Test
    void testTwoWorkersConcurrentClaim_OnlyOneGetsTask() throws Exception {
        // Given: Single PENDING task
        Task task = createTask("race-task", Complexity.LOW);

        // Given: Two workers
        Worker worker1 = createWorkerEntity("racer-1");
        Worker worker2 = createWorkerEntity("racer-2");

        // When: Both try to claim simultaneously
        CountDownLatch startLatch = new CountDownLatch(2);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<Boolean> results = Collections.synchronizedList(new ArrayList<>());

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable claimTask1 = () -> {
            startLatch.countDown();
            try {
                startLatch.await();
                Optional<Task> claimed = taskService.claimTaskForWorker(worker1);
                results.add(claimed.isPresent());
            } catch (Exception e) {
                results.add(false);
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable claimTask2 = () -> {
            startLatch.countDown();
            try {
                startLatch.await();
                Optional<Task> claimed = taskService.claimTaskForWorker(worker2);
                results.add(claimed.isPresent());
            } catch (Exception e) {
                results.add(false);
            } finally {
                doneLatch.countDown();
            }
        };

        executor.submit(claimTask1);
        executor.submit(claimTask2);

        assertTimeout(Duration.ofSeconds(10), () -> {
            doneLatch.await();
        });

        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // Then: Exactly one success
        long successCount = results.stream().filter(r -> r).count();
        assertThat(successCount).isEqualTo(1);

        // Then: Task claimed by one worker
        Task finalTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(finalTask.getAssignedWorker()).isNotNull();
        assertThat(finalTask.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void testSequentialClaims_WorkAsExpected() {
        // Given: 3 PENDING tasks
        Task task1 = createTask("seq-task-1", Complexity.LOW);
        Task task2 = createTask("seq-task-2", Complexity.LOW);
        Task task3 = createTask("seq-task-3", Complexity.LOW);

        // Given: 1 worker
        Worker worker = createWorkerEntity("sequential-worker");

        // When: Claim tasks sequentially
        Optional<Task> claimed1 = taskService.claimTaskForWorker(worker);
        Optional<Task> claimed2 = taskService.claimTaskForWorker(worker);
        Optional<Task> claimed3 = taskService.claimTaskForWorker(worker);

        // Then: All claims succeed
        assertThat(claimed1).isPresent();
        assertThat(claimed2).isPresent();
        assertThat(claimed3).isPresent();

        // Then: All different tasks
        assertThat(claimed1.get().getId()).isNotEqualTo(claimed2.get().getId());
        assertThat(claimed2.get().getId()).isNotEqualTo(claimed3.get().getId());
        assertThat(claimed1.get().getId()).isNotEqualTo(claimed3.get().getId());

        // Then: All assigned to same worker
        assertThat(claimed1.get().getAssignedWorker().getId()).isEqualTo(worker.getId());
        assertThat(claimed2.get().getAssignedWorker().getId()).isEqualTo(worker.getId());
        assertThat(claimed3.get().getAssignedWorker().getId()).isEqualTo(worker.getId());
    }

    @Test
    void testNoTasksAvailable_ReturnsEmpty() {
        // Given: Worker but no tasks
        Worker worker = createWorkerEntity("idle-worker");

        // When: Try to claim task
        Optional<Task> claimed = taskService.claimTaskForWorker(worker);

        // Then: Returns empty
        assertThat(claimed).isEmpty();
    }

    // Helper methods
    private Task createTask(String name, Complexity complexity) {
        TaskSubmissionRequestDTO request = new TaskSubmissionRequestDTO(name, complexity);
        return taskService.submitTask(request);
    }

    private Worker createWorkerEntity(String name) {
        Worker worker = new Worker();
        worker.setName(name);
        worker.setStatus(WorkerStatus.IDLE);
        worker.setCreatedAt(LocalDateTime.now());
        worker.setLastHeartbeat(LocalDateTime.now());
        return workerRepository.save(worker);
    }
}
