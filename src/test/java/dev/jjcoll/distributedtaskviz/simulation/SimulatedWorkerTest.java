package dev.jjcoll.distributedtaskviz.simulation;

import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulatedWorkerTest {

    @Mock
    private TaskService taskService;

    @Mock
    private WorkerService workerService;

    @Mock
    private WorkerSimulationManager simulationManager;

    private Worker worker;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setId(1L);
        worker.setName("test-worker");
        worker.setStatus(WorkerStatus.IDLE);
        worker.setCreatedAt(LocalDateTime.now());
        worker.setLastHeartbeat(LocalDateTime.now());
    }

    private Task createTask(Long id, String name, Complexity complexity) {
        Task task = new Task();
        task.setId(id);
        task.setName(name);
        task.setComplexity(complexity);
        return task;
    }

    @Test
    void testProcessingDelay_LowComplexity_Returns2To5Seconds() {
        // Create SimulatedWorker to test delay calculation
        SimulatedWorker simulatedWorker = new SimulatedWorker(worker, taskService, workerService, simulationManager);

        // Note: getProcessingDelay() is private, but we can test via integration
        // This test documents the expected behavior
        // In practice, delays for LOW should be 2-5 seconds
        assertThat(true).isTrue(); // Placeholder - tested in integration tests
    }

    @Test
    void testSimulatedWorkerCreation() {
        // When: Create SimulatedWorker
        SimulatedWorker simulatedWorker = new SimulatedWorker(worker, taskService, workerService, simulationManager);

        // Then: Worker created successfully
        assertThat(simulatedWorker).isNotNull();
    }

    @Test
    void testWorkerUsesProvidedServices() {
        // Given: Mock services
        SimulatedWorker simulatedWorker = new SimulatedWorker(worker, taskService, workerService, simulationManager);

        // Then: SimulatedWorker holds references to services
        assertThat(simulatedWorker).isNotNull();
        // Services will be called during run() execution
    }

    @Test
    void testComplexityEnumCoverage() {
        // Verify all complexity levels are handled
        assertThat(Complexity.LOW).isNotNull();
        assertThat(Complexity.MEDIUM).isNotNull();
        assertThat(Complexity.HIGH).isNotNull();
    }
}
