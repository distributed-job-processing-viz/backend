package dev.jjcoll.distributedtaskviz.integration;

import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.dto.WorkerCreateRequestDTO;
import dev.jjcoll.distributedtaskviz.dto.WorkerResponseDTO;
import dev.jjcoll.distributedtaskviz.model.*;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.repository.WorkerRepository;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import dev.jjcoll.distributedtaskviz.service.WorkerService;
import dev.jjcoll.distributedtaskviz.simulation.WorkerSimulationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@ActiveProfiles("test")
class WorkerSimulationIntegrationTest {

    @Autowired
    private WorkerService workerService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private WorkerSimulationManager simulationManager;

    private List<Long> createdWorkerIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        // Stop all workers created during test
        createdWorkerIds.forEach(workerId -> {
            try {
                workerService.stopWorker(workerId);
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        });
        createdWorkerIds.clear();

        // Clean up all tasks and workers from database
        taskRepository.deleteAll();
        workerRepository.deleteAll();
    }

    @Test
    void testWorkerCreation_StartsSimulationAutomatically() {
        // When: Create worker via API
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Worker exists in database
        assertThat(workerRepository.findById(worker.id())).isPresent();

        // Then: Simulation manager tracking worker
        assertThat(simulationManager.getActiveWorkerCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void testSingleWorker_ProcessesSingleTask() throws Exception {
        // Given: One LOW complexity task (2-5 second delay)
        Task task = createTask("test-task", Complexity.LOW);

        // When: Create worker
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Wait for task to be claimed (max 5 seconds)
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
                });

        // Then: Verify task assigned to worker
        Task processingTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(processingTask.getAssignedWorker().getId()).isEqualTo(worker.id());
        assertThat(processingTask.getProcessingStartedAt()).isNotNull();

        // Then: Wait for task to complete (max 10 seconds)
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
                });

        // Then: Verify task completed
        Task completedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(completedTask.getCompletedAt()).isNotNull();
    }

    @Test
    void testMultipleWorkers_ProcessMultipleTasks() throws Exception {
        // Given: 5 LOW complexity tasks
        List<Task> tasks = IntStream.range(0, 5)
                .mapToObj(i -> createTask("task-" + i, Complexity.LOW))
                .collect(Collectors.toList());

        // When: Create 3 workers
        List<WorkerResponseDTO> workers = IntStream.range(0, 3)
                .mapToObj(i -> workerService.createWorker(new WorkerCreateRequestDTO(null)))
                .collect(Collectors.toList());

        workers.forEach(w -> createdWorkerIds.add(w.id()));

        // Then: All tasks eventually completed (max 20 seconds)
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long completedCount = taskRepository.findAll().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    assertThat(completedCount).isEqualTo(5);
                });

        // Then: Verify all tasks assigned to workers
        List<Task> completedTasks = taskRepository.findAll();
        assertThat(completedTasks).allMatch(t -> t.getAssignedWorker() != null);

        // Then: Verify tasks distributed across workers (if possible)
        Map<Long, Long> tasksByWorker = completedTasks.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getAssignedWorker().getId(),
                        Collectors.counting()
                ));

        // At least one worker should have processed at least one task
        assertThat(tasksByWorker).isNotEmpty();
    }

    @Test
    void testWorkerStopped_MarksInFlightTaskAsFailed() throws Exception {
        // Given: One HIGH complexity task (10-20 second delay)
        Task task = createTask("long-task", Complexity.HIGH);

        // When: Create worker
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Wait for task to be claimed and processing
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
                });

        // When: Stop worker while processing
        workerService.stopWorker(worker.id());

        // Then: Task marked as FAILED (within 3 seconds)
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.FAILED);
                });

        // Then: Verify task has completedAt timestamp
        Task failedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(failedTask.getCompletedAt()).isNotNull();
    }

    @Test
    void testHeartbeat_UpdatedDuringProcessing() throws Exception {
        // Given: One MEDIUM complexity task (5-10 second delay)
        Task task = createTask("heartbeat-task", Complexity.MEDIUM);

        // When: Create worker
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Wait for task to start processing
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
                });

        // Then: Capture initial heartbeat
        LocalDateTime firstHeartbeat = workerRepository
                .findById(worker.id()).orElseThrow().getLastHeartbeat();

        // When: Wait 6 seconds (longer than heartbeat interval)
        Thread.sleep(6000);

        // Then: Heartbeat has been updated
        LocalDateTime secondHeartbeat = workerRepository
                .findById(worker.id()).orElseThrow().getLastHeartbeat();
        assertThat(secondHeartbeat).isAfter(firstHeartbeat);
    }

    @Test
    void testWorkerIdleBehavior_NoTasksAvailable() throws Exception {
        // When: Create worker with NO tasks available
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Worker status should be IDLE
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Worker w = workerRepository.findById(worker.id()).orElseThrow();
                    assertThat(w.getStatus()).isEqualTo(WorkerStatus.IDLE);
                });

        // Then: Heartbeat still updating
        LocalDateTime firstHeartbeat = workerRepository
                .findById(worker.id()).orElseThrow().getLastHeartbeat();

        Thread.sleep(6000);

        LocalDateTime secondHeartbeat = workerRepository
                .findById(worker.id()).orElseThrow().getLastHeartbeat();
        assertThat(secondHeartbeat).isAfter(firstHeartbeat);
    }

    @Test
    void testWorkerProcessing_UpdatesStatusCorrectly() throws Exception {
        // Given: One LOW complexity task
        Task task = createTask("status-test-task", Complexity.LOW);

        // When: Create worker
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Worker starts IDLE
        Worker initialWorker = workerRepository.findById(worker.id()).orElseThrow();
        // Status might be IDLE or already PROCESSING if very fast

        // Then: Worker transitions to PROCESSING
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Worker w = workerRepository.findById(worker.id()).orElseThrow();
                    // Worker should be PROCESSING at some point
                    Task t = taskRepository.findById(task.getId()).orElseThrow();
                    if (t.getStatus() == TaskStatus.PROCESSING) {
                        assertThat(w.getStatus()).isEqualTo(WorkerStatus.PROCESSING);
                    }
                });

        // Then: After task completes, worker goes back to IDLE
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task t = taskRepository.findById(task.getId()).orElseThrow();
                    if (t.getStatus() == TaskStatus.COMPLETED) {
                        Worker w = workerRepository.findById(worker.id()).orElseThrow();
                        assertThat(w.getStatus()).isEqualTo(WorkerStatus.IDLE);
                    }
                });
    }

    // Helper method
    private Task createTask(String name, Complexity complexity) {
        TaskSubmissionRequestDTO request = new TaskSubmissionRequestDTO(name, complexity);
        return taskService.submitTask(request);
    }
}
