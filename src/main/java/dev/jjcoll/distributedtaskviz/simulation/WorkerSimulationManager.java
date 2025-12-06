package dev.jjcoll.distributedtaskviz.simulation;

import dev.jjcoll.distributedtaskviz.model.EngineState;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import dev.jjcoll.distributedtaskviz.service.WorkerService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Manages the lifecycle of simulated workers using a shared thread pool.
 * This component is responsible for:
 * - Creating and managing a shared ExecutorService thread pool
 * - Starting worker simulations by submitting SimulatedWorker tasks to the pool
 * - Stopping individual workers gracefully
 * - Shutting down the entire pool on application shutdown
 */
@Component
public class WorkerSimulationManager {

    private static final Logger logger = LoggerFactory.getLogger(WorkerSimulationManager.class);

    private final ExecutorService executorService;
    private final TaskService taskService;
    private final WorkerService workerService;

    // Engine state control
    private volatile EngineState engineState;

    // Map to track active workers: workerId -> Future
    private final Map<Long, Future<?>> activeWorkers = new ConcurrentHashMap<>();

    // List to track workers waiting for engine to start
    private final List<Worker> pendingWorkers = new CopyOnWriteArrayList<>();

    // Thread pool configuration from application.properties
    @Value("${worker.simulation.thread-pool.core-size:5}")
    private int corePoolSize;

    @Value("${worker.simulation.thread-pool.max-size:10}")
    private int maxPoolSize;

    @Value("${worker.simulation.thread-pool.queue-capacity:100}")
    private int queueCapacity;

    public WorkerSimulationManager(TaskService taskService, WorkerService workerService,
                                   @Value("${worker.simulation.thread-pool.core-size:5}") int corePoolSize,
                                   @Value("${worker.simulation.thread-pool.max-size:10}") int maxPoolSize,
                                   @Value("${worker.simulation.thread-pool.queue-capacity:100}") int queueCapacity,
                                   @Value("${worker.simulation.auto-start:false}") boolean autoStart) {
        this.taskService = taskService;
        this.workerService = workerService;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;

        // Set initial engine state based on configuration
        if (autoStart) {
            this.engineState = EngineState.RUNNING;
            logger.info("Engine auto-started (configured)");
        } else {
            this.engineState = EngineState.STOPPED;
            logger.info("Engine started in STOPPED state (manual control enabled)");
        }

        // Create thread pool with bounded queue
        this.executorService = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("worker-sim-" + (++counter));
                        thread.setDaemon(true); // Daemon threads don't block JVM shutdown
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // Backpressure policy
        );

        logger.info("WorkerSimulationManager initialized with pool size: core={}, max={}, queue={}",
                corePoolSize, maxPoolSize, queueCapacity);
    }

    /**
     * Starts a simulated worker. If engine is STOPPED, worker is queued.
     * If engine is RUNNING, worker starts immediately.
     *
     * @param worker The worker entity to simulate
     */
    public void startWorker(Worker worker) {
        if (engineState == EngineState.STOPPED) {
            // Engine stopped - queue worker for later
            pendingWorkers.add(worker);
            logger.info("Worker {} ({}) queued (engine stopped)", worker.getId(), worker.getName());
            return;
        }

        // Engine running - start immediately
        startWorkerInternal(worker);
    }

    /**
     * Internal method to actually start a worker simulation.
     *
     * @param worker The worker entity to simulate
     */
    private void startWorkerInternal(Worker worker) {
        Long workerId = worker.getId();

        if (activeWorkers.containsKey(workerId)) {
            logger.warn("Worker {} is already running", workerId);
            return;
        }

        SimulatedWorker simulatedWorker = new SimulatedWorker(
                worker,
                taskService,
                workerService,
                this
        );

        Future<?> future = executorService.submit(simulatedWorker);
        activeWorkers.put(workerId, future);

        logger.info("Started simulation for worker {} ({})", workerId, worker.getName());
    }

    /**
     * Stops a specific worker by canceling its future and interrupting its thread.
     * Also removes from pending workers list if queued.
     * The SimulatedWorker checks for interruption and shuts down gracefully.
     *
     * @param workerId The ID of the worker to stop
     */
    public void stopWorker(Long workerId) {
        // Remove from pending list if present
        pendingWorkers.removeIf(w -> w.getId().equals(workerId));

        // Remove from active workers
        Future<?> future = activeWorkers.remove(workerId);

        if (future != null) {
            // Interrupt the worker thread (SimulatedWorker will handle graceful shutdown)
            future.cancel(true);
            logger.info("Stopped simulation for worker {}", workerId);
        } else {
            logger.debug("Worker {} not in active workers (may have been pending)", workerId);
        }
    }

    /**
     * Called on application shutdown to gracefully stop all workers.
     * Uses @PreDestroy to ensure this runs before Spring context is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down WorkerSimulationManager...");

        // Stop all active workers
        activeWorkers.keySet().forEach(this::stopWorker);

        // Shutdown thread pool gracefully
        executorService.shutdown();

        try {
            // Wait up to 30 seconds for workers to finish in-flight tasks
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Thread pool did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.error("Shutdown interrupted", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("WorkerSimulationManager shutdown complete");
    }

    /**
     * Returns the number of currently active workers.
     */
    public int getActiveWorkerCount() {
        return activeWorkers.size();
    }

    /**
     * Starts the processing engine. All pending workers are activated
     * and begin processing tasks.
     */
    public synchronized void startEngine() {
        if (engineState == EngineState.RUNNING) {
            logger.warn("Engine is already running");
            return;
        }

        logger.info("Starting processing engine...");
        engineState = EngineState.RUNNING;

        // Start all pending workers
        List<Worker> workersToStart = new ArrayList<>(pendingWorkers);
        pendingWorkers.clear();

        workersToStart.forEach(this::startWorkerInternal);

        logger.info("Engine started. Activated {} workers", workersToStart.size());
    }

    /**
     * Pauses the processing engine. Worker threads remain alive but idle.
     * Tasks are not processed until engine is resumed.
     */
    public synchronized void pauseEngine() {
        if (engineState == EngineState.PAUSED) {
            logger.warn("Engine is already paused");
            return;
        }

        if (engineState == EngineState.STOPPED) {
            logger.warn("Cannot pause: engine is stopped");
            return;
        }

        logger.info("Pausing processing engine...");
        engineState = EngineState.PAUSED;

        logger.info("Engine paused. {} workers idle", activeWorkers.size());
    }

    /**
     * Resumes the processing engine from paused state.
     * Workers continue processing tasks.
     */
    public synchronized void resumeEngine() {
        if (engineState != EngineState.PAUSED) {
            logger.warn("Cannot resume: engine is not paused (current state: {})", engineState);
            return;
        }

        logger.info("Resuming processing engine...");
        engineState = EngineState.RUNNING;

        logger.info("Engine resumed. {} workers active", activeWorkers.size());
    }

    /**
     * Stops the processing engine. All active workers are terminated.
     * In-flight tasks will be marked as FAILED.
     */
    public synchronized void stopEngine() {
        if (engineState == EngineState.STOPPED) {
            logger.warn("Engine is already stopped");
            return;
        }

        logger.info("Stopping processing engine...");
        engineState = EngineState.STOPPED;

        // Stop all active workers
        List<Long> workerIds = new ArrayList<>(activeWorkers.keySet());
        workerIds.forEach(this::stopWorker);

        logger.info("Engine stopped. Terminated {} workers", workerIds.size());
    }

    /**
     * Returns the current state of the processing engine.
     *
     * @return Current engine state (STOPPED or RUNNING)
     */
    public EngineState getEngineState() {
        return engineState;
    }

    /**
     * Returns the number of workers waiting to be activated.
     *
     * @return Number of pending workers
     */
    public int getPendingWorkerCount() {
        return pendingWorkers.size();
    }
}
