package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.dto.WorkerCreateRequestDTO;
import dev.jjcoll.distributedtaskviz.dto.WorkerResponseDTO;
import dev.jjcoll.distributedtaskviz.mappers.WorkerMapper;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import dev.jjcoll.distributedtaskviz.repository.WorkerRepository;
import dev.jjcoll.distributedtaskviz.simulation.WorkerSimulationManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;
    private final WorkerSimulationManager simulationManager;
    private static final String WORKER_NAME_PREFIX = "worker-";

    public WorkerService(WorkerRepository workerRepository, WorkerMapper workerMapper,
                         @Lazy WorkerSimulationManager simulationManager) {
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
        this.simulationManager = simulationManager;
    }

    /**
     * Creates a new worker with auto-generated name if not provided.
     * Also starts the worker simulation thread.
     *
     * @param request the worker creation request
     * @return the created worker as a DTO
     */
    public WorkerResponseDTO createWorker(WorkerCreateRequestDTO request) {
        Worker worker = workerMapper.toEntity(request);

        // Auto-generate name if not provided
        if (worker.getName() == null || worker.getName().isBlank()) {
            worker.setName(generateWorkerName());
        }

        Worker savedWorker = workerRepository.save(worker);

        // Start the worker simulation
        simulationManager.startWorker(savedWorker);

        return workerMapper.toDto(savedWorker);
    }

    /**
     * Retrieves a worker by ID.
     *
     * @param id the worker ID
     * @return an Optional containing the worker DTO if found
     */
    public Optional<WorkerResponseDTO> getWorker(Long id) {
        return workerRepository.findById(id)
                .map(workerMapper::toDto);
    }

    /**
     * Retrieves all workers.
     *
     * @return list of all workers as DTOs
     */
    public List<WorkerResponseDTO> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(workerMapper::toDto)
                .toList();
    }

    /**
     * Stops a worker by setting its status to STOPPED and shutting down its thread.
     *
     * @param id the worker ID
     * @return an Optional containing the updated worker DTO if found
     */
    public Optional<WorkerResponseDTO> stopWorker(Long id) {
        Optional<Worker> workerOpt = workerRepository.findById(id);

        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            worker.setStatus(WorkerStatus.STOPPED);
            Worker savedWorker = workerRepository.save(worker);

            // Stop the worker simulation thread
            simulationManager.stopWorker(id);

            return Optional.of(workerMapper.toDto(savedWorker));
        }

        return Optional.empty();
    }

    /**
     * Updates worker's heartbeat timestamp.
     * Called periodically by SimulatedWorker.
     *
     * @param workerId the worker ID
     */
    @Transactional
    public void updateHeartbeat(Long workerId) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        worker.setLastHeartbeat(LocalDateTime.now());
        workerRepository.save(worker);
    }

    /**
     * Updates worker status.
     * Used by SimulatedWorker to update status (IDLE/PROCESSING).
     *
     * @param workerId the worker ID
     * @param status the new status
     */
    @Transactional
    public void updateWorkerStatus(Long workerId, WorkerStatus status) {
        Worker worker = workerRepository.findById(workerId)
                .orElseThrow(() -> new RuntimeException("Worker not found: " + workerId));

        worker.setStatus(status);
        workerRepository.save(worker);
    }

    /**
     * Generates a unique worker name based on existing workers.
     *
     * @return a unique worker name (e.g., "worker-1", "worker-2")
     */
    private String generateWorkerName() {
        long count = workerRepository.countByNameStartingWith(WORKER_NAME_PREFIX);
        return WORKER_NAME_PREFIX + (count + 1);
    }
}