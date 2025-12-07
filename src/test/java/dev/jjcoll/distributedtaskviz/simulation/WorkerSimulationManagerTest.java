package dev.jjcoll.distributedtaskviz.simulation;

import dev.jjcoll.distributedtaskviz.model.EngineState;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import dev.jjcoll.distributedtaskviz.service.WorkerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerSimulationManagerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private WorkerService workerService;

    private WorkerSimulationManager manager;

    private Worker createTestWorker(Long id, String name) {
        Worker worker = new Worker();
        worker.setId(id);
        worker.setName(name);
        worker.setStatus(WorkerStatus.IDLE);
        worker.setCreatedAt(LocalDateTime.now());
        worker.setLastHeartbeat(LocalDateTime.now());
        return worker;
    }

    @BeforeEach
    void setUp() {
        // Create manager with test configuration (max 10 workers, auto-start = false for manual control tests)
        manager = new WorkerSimulationManager(taskService, workerService, 10, false);
    }

    @Test
    void testManagerInitialization() {
        // Then: Manager initialized successfully
        assertThat(manager).isNotNull();
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testStartWorker_CreatesAndSubmitsSimulatedWorker() {
        // Given: Engine is RUNNING and a worker entity
        manager.startEngine();
        Worker worker = createTestWorker(1L, "worker-1");

        // When: startWorker() called
        manager.startWorker(worker);

        // Then: Worker added to activeWorkers map
        assertThat(manager.getActiveWorkerCount()).isEqualTo(1);
    }

    @Test
    void testStartWorker_AlreadyRunning_DoesNotStartAgain() {
        // Given: Engine RUNNING with worker already running
        manager.startEngine();
        Worker worker = createTestWorker(1L, "worker-1");
        manager.startWorker(worker);

        // When: startWorker() called again
        int countBefore = manager.getActiveWorkerCount();
        manager.startWorker(worker);

        // Then: Worker count unchanged (warning logged)
        assertThat(manager.getActiveWorkerCount()).isEqualTo(countBefore);
    }

    @Test
    void testStopWorker_RemovesFromMap() {
        // Given: Engine RUNNING with worker in activeWorkers map
        manager.startEngine();
        Worker worker = createTestWorker(1L, "worker-1");
        manager.startWorker(worker);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(1);

        // When: stopWorker() called
        manager.stopWorker(1L);

        // Then: Worker removed from activeWorkers map
        // Note: May take a moment for thread to actually stop
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testStopWorker_NotFound_DoesNotThrowException() {
        // Given: Worker not in activeWorkers map
        // When: stopWorker() called
        manager.stopWorker(999L);

        // Then: No exception thrown (warning logged)
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testGetActiveWorkerCount() {
        // Given: Engine RUNNING with multiple workers
        manager.startEngine();
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");
        Worker worker3 = createTestWorker(3L, "worker-3");

        // When: Workers started
        manager.startWorker(worker1);
        manager.startWorker(worker2);
        manager.startWorker(worker3);

        // Then: Count reflects active workers
        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);

        // When: One worker stopped
        manager.stopWorker(2L);

        // Then: Count decreases
        assertThat(manager.getActiveWorkerCount()).isEqualTo(2);
    }

    @Test
    void testMultipleWorkersStartAndStop() {
        // Given: Engine RUNNING with 5 workers
        manager.startEngine();
        for (int i = 1; i <= 5; i++) {
            Worker worker = createTestWorker((long) i, "worker-" + i);
            manager.startWorker(worker);
        }

        // Then: All workers active
        assertThat(manager.getActiveWorkerCount()).isEqualTo(5);

        // When: Stop all workers
        for (int i = 1; i <= 5; i++) {
            manager.stopWorker((long) i);
        }

        // Then: No active workers
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    // ========== Engine Control Tests ==========

    @Test
    void testEngineInitialization_AutoStartFalse_EngineStoppedInitially() {
        // Given: Manager created with autoStart=false (in setUp())
        // Then: Engine state is STOPPED
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);
    }

    @Test
    void testEngineInitialization_AutoStartTrue_EngineRunningInitially() {
        // Given: Manager created with autoStart=true
        WorkerSimulationManager autoStartManager = new WorkerSimulationManager(
            taskService, workerService, 10, true
        );

        // Then: Engine state is RUNNING
        assertThat(autoStartManager.getEngineState()).isEqualTo(EngineState.RUNNING);

        // Cleanup
        autoStartManager.shutdown();
    }

    @Test
    void testStartWorker_WhenEngineStopped_WorkerQueued() {
        // Given: Engine is STOPPED (default from setUp)
        Worker worker = createTestWorker(1L, "worker-1");

        // When: Start worker
        manager.startWorker(worker);

        // Then: Worker NOT in active workers (queued instead)
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
        assertThat(manager.getPendingWorkerCount()).isEqualTo(1);
    }

    @Test
    void testStartWorker_WhenEngineRunning_WorkerStartsImmediately() {
        // Given: Engine is RUNNING
        manager.startEngine();
        Worker worker = createTestWorker(1L, "worker-1");

        // When: Start worker
        manager.startWorker(worker);

        // Then: Worker in active workers
        assertThat(manager.getActiveWorkerCount()).isEqualTo(1);
        assertThat(manager.getPendingWorkerCount()).isEqualTo(0);
    }

    @Test
    void testStartEngine_ActivatesPendingWorkers() {
        // Given: Engine STOPPED with 3 pending workers
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");
        Worker worker3 = createTestWorker(3L, "worker-3");

        manager.startWorker(worker1);
        manager.startWorker(worker2);
        manager.startWorker(worker3);

        assertThat(manager.getPendingWorkerCount()).isEqualTo(3);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);

        // When: Start engine
        manager.startEngine();

        // Then: All pending workers activated
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(manager.getPendingWorkerCount()).isEqualTo(0);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);
    }

    @Test
    void testStartEngine_AlreadyRunning_NoEffect() {
        // Given: Engine already RUNNING
        manager.startEngine();
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);

        // When: Start engine again
        manager.startEngine();

        // Then: Still RUNNING (idempotent operation)
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
    }

    @Test
    void testStopEngine_StopsAllActiveWorkers() {
        // Given: Engine RUNNING with 3 active workers
        manager.startEngine();
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");
        Worker worker3 = createTestWorker(3L, "worker-3");

        manager.startWorker(worker1);
        manager.startWorker(worker2);
        manager.startWorker(worker3);

        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);

        // When: Stop engine
        manager.stopEngine();

        // Then: All workers stopped
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testStopEngine_AlreadyStopped_NoEffect() {
        // Given: Engine already STOPPED
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);

        // When: Stop engine
        manager.stopEngine();

        // Then: Still STOPPED (idempotent operation)
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);
    }

    @Test
    void testStopWorker_RemovesFromPendingList() {
        // Given: Engine STOPPED with 2 pending workers
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");

        manager.startWorker(worker1);
        manager.startWorker(worker2);

        assertThat(manager.getPendingWorkerCount()).isEqualTo(2);

        // When: Stop one pending worker
        manager.stopWorker(1L);

        // Then: Worker removed from pending list
        assertThat(manager.getPendingWorkerCount()).isEqualTo(1);
    }

    @Test
    void testEngineLifecycle_FullWorkflow() {
        // Test: Complete engine lifecycle

        // 1. Engine starts STOPPED
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);

        // 2. Create workers while engine stopped (queued)
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");
        manager.startWorker(worker1);
        manager.startWorker(worker2);
        assertThat(manager.getPendingWorkerCount()).isEqualTo(2);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);

        // 3. Start engine (activates queued workers)
        manager.startEngine();
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(manager.getPendingWorkerCount()).isEqualTo(0);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(2);

        // 4. Create new worker while engine running (starts immediately)
        Worker worker3 = createTestWorker(3L, "worker-3");
        manager.startWorker(worker3);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);

        // 5. Pause engine (workers remain active but idle)
        manager.pauseEngine();
        assertThat(manager.getEngineState()).isEqualTo(EngineState.PAUSED);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);

        // 6. Resume engine
        manager.resumeEngine();
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(3);

        // 7. Stop engine (terminates all workers)
        manager.stopEngine();
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(0);
    }

    @Test
    void testPauseEngine_FromRunning() {
        // Given: Engine RUNNING with 2 workers
        manager.startEngine();
        Worker worker1 = createTestWorker(1L, "worker-1");
        Worker worker2 = createTestWorker(2L, "worker-2");
        manager.startWorker(worker1);
        manager.startWorker(worker2);

        // When: Pause engine
        manager.pauseEngine();

        // Then: State is PAUSED, workers still active
        assertThat(manager.getEngineState()).isEqualTo(EngineState.PAUSED);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(2);
    }

    @Test
    void testResumeEngine_FromPaused() {
        // Given: Engine PAUSED
        manager.startEngine();
        Worker worker = createTestWorker(1L, "worker-1");
        manager.startWorker(worker);
        manager.pauseEngine();

        // When: Resume engine
        manager.resumeEngine();

        // Then: State is RUNNING
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
        assertThat(manager.getActiveWorkerCount()).isEqualTo(1);
    }

    @Test
    void testPauseEngine_WhenStopped_NoEffect() {
        // Given: Engine STOPPED
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);

        // When: Try to pause
        manager.pauseEngine();

        // Then: Still STOPPED (can't pause stopped engine)
        assertThat(manager.getEngineState()).isEqualTo(EngineState.STOPPED);
    }

    @Test
    void testResumeEngine_WhenNotPaused_NoEffect() {
        // Given: Engine RUNNING (not paused)
        manager.startEngine();

        // When: Try to resume
        manager.resumeEngine();

        // Then: Still RUNNING (warning logged)
        assertThat(manager.getEngineState()).isEqualTo(EngineState.RUNNING);
    }
}
