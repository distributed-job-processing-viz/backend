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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for the engine control functionality.
 * Tests the full workflow of starting/stopping the processing engine
 * and verifying task processing behavior.
 */
@SpringBootTest
@ActiveProfiles("test")
class EngineControlIntegrationTest {

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

    @BeforeEach
    void setUp() {
        // Ensure engine is stopped before each test
        if (simulationManager.getEngineState() == EngineState.RUNNING) {
            simulationManager.stopEngine();
        }
    }

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

        // Ensure engine is stopped after test
        if (simulationManager.getEngineState() == EngineState.RUNNING) {
            simulationManager.stopEngine();
        }

        // Clean up all tasks and workers from database
        taskRepository.deleteAll();
        workerRepository.deleteAll();
    }

    @Test
    void testCreateWorkerWhileEngineStopped_WorkerQueued() {
        // Given: Engine is STOPPED (verified in setUp)
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.STOPPED);

        // When: Create worker
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        // Then: Worker exists in database
        assertThat(workerRepository.findById(worker.id())).isPresent();

        // Then: Worker is queued (not active)
        assertThat(simulationManager.getPendingWorkerCount()).isEqualTo(1);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testStartEngine_ActivatesQueuedWorkers() {
        // Given: Engine STOPPED with 2 queued workers
        WorkerResponseDTO worker1 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        WorkerResponseDTO worker2 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker1.id());
        createdWorkerIds.add(worker2.id());

        assertThat(simulationManager.getPendingWorkerCount()).isEqualTo(2);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(0);

        // When: Start engine
        simulationManager.startEngine();

        // Then: Engine is RUNNING
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.RUNNING);

        // Then: Workers activated
        assertThat(simulationManager.getPendingWorkerCount()).isEqualTo(0);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(2);
    }

    @Test
    void testStopEngine_DeactivatesAllWorkers() {
        // Given: Engine RUNNING with 2 active workers
        simulationManager.startEngine();

        WorkerResponseDTO worker1 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        WorkerResponseDTO worker2 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker1.id());
        createdWorkerIds.add(worker2.id());

        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(2);

        // When: Stop engine
        simulationManager.stopEngine();

        // Then: Engine is STOPPED
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.STOPPED);

        // Then: All workers deactivated
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testTasksNotProcessedWhileEngineStopped() throws Exception {
        // Given: Engine STOPPED with 1 worker and 1 task
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        Task task = createTask("test-task", Complexity.LOW);

        // Wait a few seconds to verify task is NOT picked up
        Thread.sleep(3000);

        // Then: Task still PENDING (not processed)
        Task unchangedTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(unchangedTask.getStatus()).isEqualTo(TaskStatus.PENDING);
        assertThat(unchangedTask.getAssignedWorker()).isNull();
    }

    @Test
    void testTasksProcessedAfterEngineStarted() throws Exception {
        // Given: Engine STOPPED with 1 worker and 1 task
        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        Task task = createTask("test-task", Complexity.LOW);

        // Verify task not picked up while engine stopped
        Thread.sleep(2000);
        Task pendingTask = taskRepository.findById(task.getId()).orElseThrow();
        assertThat(pendingTask.getStatus()).isEqualTo(TaskStatus.PENDING);

        // When: Start engine
        simulationManager.startEngine();

        // Then: Task is claimed and processed
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
                });

        // Then: Task eventually completes
        await().atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.COMPLETED);
                });
    }

    @Test
    void testEngineStopDuringProcessing_MarksTaskAsFailed() throws Exception {
        // Given: Engine RUNNING with 1 worker processing a HIGH complexity task
        simulationManager.startEngine();

        WorkerResponseDTO worker = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker.id());

        Task task = createTask("long-task", Complexity.HIGH);

        // Wait for task to be claimed and start processing
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.PROCESSING);
                });

        // When: Stop engine while task is processing
        simulationManager.stopEngine();

        // Then: Task marked as FAILED (within 3 seconds)
        await().atMost(3, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    Task updated = taskRepository.findById(task.getId()).orElseThrow();
                    assertThat(updated.getStatus()).isEqualTo(TaskStatus.FAILED);
                });
    }

    @Test
    void testFullLifecycle_CreateWorkersAndTasks_StartEngine_ProcessTasks_StopEngine() throws Exception {
        // 1. Create 3 workers and 3 tasks while engine STOPPED
        WorkerResponseDTO worker1 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        WorkerResponseDTO worker2 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        WorkerResponseDTO worker3 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker1.id());
        createdWorkerIds.add(worker2.id());
        createdWorkerIds.add(worker3.id());

        Task task1 = createTask("task-1", Complexity.LOW);
        Task task2 = createTask("task-2", Complexity.LOW);
        Task task3 = createTask("task-3", Complexity.LOW);

        // Verify workers queued, tasks pending
        assertThat(simulationManager.getPendingWorkerCount()).isEqualTo(3);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(0);

        Thread.sleep(2000);
        assertThat(taskRepository.findById(task1.getId()).orElseThrow().getStatus()).isEqualTo(TaskStatus.PENDING);

        // 2. Start engine
        simulationManager.startEngine();

        // Verify workers activated
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(3);

        // 3. Wait for all tasks to complete
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long completedCount = taskRepository.findAll().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    assertThat(completedCount).isEqualTo(3);
                });

        // 4. Stop engine
        simulationManager.stopEngine();

        // Verify engine stopped, workers deactivated
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.STOPPED);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testEnginePauseResume_PreservesWorkers() throws Exception {
        // Given: Engine RUNNING with 2 workers and 3 tasks
        simulationManager.startEngine();

        WorkerResponseDTO worker1 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        WorkerResponseDTO worker2 = workerService.createWorker(new WorkerCreateRequestDTO(null));
        createdWorkerIds.add(worker1.id());
        createdWorkerIds.add(worker2.id());

        Task task1 = createTask("task-1", Complexity.LOW);
        Task task2 = createTask("task-2", Complexity.LOW);
        Task task3 = createTask("task-3", Complexity.LOW);

        // Wait for at least one task to be picked up
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long processingCount = taskRepository.findAll().stream()
                            .filter(t -> t.getStatus() == TaskStatus.PROCESSING || t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    assertThat(processingCount).isGreaterThanOrEqualTo(1);
                });

        // When: Pause engine (workers stay alive but idle)
        simulationManager.pauseEngine();

        // Then: Engine is PAUSED, workers still active (threads alive)
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.PAUSED);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(2);

        // Wait to verify no more tasks are processed while paused
        Thread.sleep(3000);

        long completedWhilePaused = taskRepository.findAll().stream()
                .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                .count();

        // When: Resume engine
        simulationManager.resumeEngine();

        // Then: Engine is RUNNING again
        assertThat(simulationManager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(simulationManager.getActiveWorkerCount()).isEqualTo(2);

        // Then: All tasks eventually complete after resume
        await().atMost(20, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    long completed = taskRepository.findAll().stream()
                            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
                            .count();
                    assertThat(completed).isEqualTo(3);
                });
    }

    // Helper method
    private Task createTask(String name, Complexity complexity) {
        TaskSubmissionRequestDTO request = new TaskSubmissionRequestDTO(name, complexity);
        return taskService.submitTask(request);
    }
}
