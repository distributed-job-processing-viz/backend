package dev.jjcoll.distributedtaskviz.simulation;

import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.EngineState;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import dev.jjcoll.distributedtaskviz.service.WorkerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Random;

/**
 * Simulates a worker that continuously polls for tasks, processes them, and updates heartbeat.
 * Implements Runnable to be executed by WorkerSimulationManager's thread pool.
 */
public class SimulatedWorker implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SimulatedWorker.class);

    private final Worker worker;
    private final TaskService taskService;
    private final WorkerService workerService;
    private final WorkerSimulationManager simulationManager;
    private final Random random = new Random();

    // Heartbeat configuration
    private static final long HEARTBEAT_INTERVAL_MS = 5000; // 5 seconds
    private long lastHeartbeatTime = System.currentTimeMillis();

    // Polling configuration
    private static final long POLL_INTERVAL_MS = 1000; // 1 second between polls

    // Processing delays based on complexity (in seconds)
    private static final int LOW_MIN = 2, LOW_MAX = 5;
    private static final int MEDIUM_MIN = 5, MEDIUM_MAX = 10;
    private static final int HIGH_MIN = 10, HIGH_MAX = 20;

    public SimulatedWorker(Worker worker, TaskService taskService, WorkerService workerService,
                           WorkerSimulationManager simulationManager) {
        this.worker = worker;
        this.taskService = taskService;
        this.workerService = workerService;
        this.simulationManager = simulationManager;
    }

    @Override
    public void run() {
        logger.info("Worker {} ({}) starting processing loop", worker.getId(), worker.getName());

        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Update heartbeat if needed
                updateHeartbeatIfNeeded();

                // Check if engine is paused - if so, sleep briefly and continue
                if (simulationManager.getEngineState() == EngineState.PAUSED) {
                    workerService.updateWorkerStatus(worker.getId(), WorkerStatus.IDLE);
                    Thread.sleep(100); // Brief sleep while paused
                    continue;
                }

                // Try to claim a task
                Optional<Task> taskOpt = taskService.claimTaskForWorker(worker);

                if (taskOpt.isPresent()) {
                    processTask(taskOpt.get());
                } else {
                    // No tasks available, set worker to IDLE and wait before polling again
                    workerService.updateWorkerStatus(worker.getId(), WorkerStatus.IDLE);
                    Thread.sleep(POLL_INTERVAL_MS);
                }
            }
        } catch (InterruptedException e) {
            logger.info("Worker {} interrupted, shutting down gracefully", worker.getId());
            Thread.currentThread().interrupt(); // Restore interrupt status
        } catch (Exception e) {
            logger.error("Worker {} encountered unexpected error", worker.getId(), e);
        } finally {
            // Ensure worker is marked as STOPPED in database
            try {
                workerService.updateWorkerStatus(worker.getId(), WorkerStatus.STOPPED);
                logger.info("Worker {} stopped", worker.getId());
            } catch (Exception e) {
                logger.error("Failed to update worker {} status to STOPPED", worker.getId(), e);
            }
        }
    }

    /**
     * Processes a single task: simulates work, updates status.
     */
    private void processTask(Task task) throws InterruptedException {
        Long taskId = task.getId();

        try {
            logger.info("Worker {} processing task {} ({})",
                    worker.getId(), taskId, task.getName());

            // Update worker status to PROCESSING
            workerService.updateWorkerStatus(worker.getId(), WorkerStatus.PROCESSING);

            // Simulate processing based on complexity
            int delaySeconds = getProcessingDelay(task.getComplexity());
            simulateProcessing(delaySeconds);

            // Mark task as completed
            taskService.completeTask(taskId);
            logger.info("Worker {} completed task {} in {}s",
                    worker.getId(), taskId, delaySeconds);

        } catch (InterruptedException e) {
            // Worker was interrupted during processing
            logger.warn("Worker {} interrupted while processing task {}, marking as FAILED",
                    worker.getId(), taskId);
            taskService.failTask(taskId, "Worker interrupted during processing");
            throw e; // Propagate to stop worker loop

        } catch (Exception e) {
            // Unexpected error during processing
            logger.error("Worker {} failed to process task {}", worker.getId(), taskId, e);
            taskService.failTask(taskId, "Unexpected error: " + e.getMessage());
        }
    }

    /**
     * Simulates processing delay based on task complexity.
     * Uses Thread.sleep with periodic interrupt checks and heartbeat updates.
     */
    private void simulateProcessing(int delaySeconds) throws InterruptedException {
        long endTime = System.currentTimeMillis() + (delaySeconds * 1000L);

        while (System.currentTimeMillis() < endTime) {
            // Check for interruption
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }

            // Update heartbeat during long processing
            updateHeartbeatIfNeeded();

            // Sleep in small increments for responsive interruption
            long remaining = endTime - System.currentTimeMillis();
            if (remaining > 0) {
                Thread.sleep(Math.min(500, remaining));
            }
        }
    }

    /**
     * Returns random processing delay based on complexity.
     */
    private int getProcessingDelay(Complexity complexity) {
        return switch (complexity) {
            case LOW -> LOW_MIN + random.nextInt(LOW_MAX - LOW_MIN + 1);
            case MEDIUM -> MEDIUM_MIN + random.nextInt(MEDIUM_MAX - MEDIUM_MIN + 1);
            case HIGH -> HIGH_MIN + random.nextInt(HIGH_MAX - HIGH_MIN + 1);
        };
    }

    /**
     * Updates heartbeat if 5 seconds have elapsed since last update.
     */
    private void updateHeartbeatIfNeeded() {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastHeartbeatTime >= HEARTBEAT_INTERVAL_MS) {
            try {
                workerService.updateHeartbeat(worker.getId());
                lastHeartbeatTime = currentTime;
                logger.debug("Worker {} heartbeat updated", worker.getId());
            } catch (Exception e) {
                logger.error("Failed to update heartbeat for worker {}", worker.getId(), e);
            }
        }
    }
}
