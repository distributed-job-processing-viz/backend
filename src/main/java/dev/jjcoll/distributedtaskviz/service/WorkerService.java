package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.dto.WorkerCreateRequestDTO;
import dev.jjcoll.distributedtaskviz.dto.WorkerResponseDTO;
import dev.jjcoll.distributedtaskviz.mappers.WorkerMapper;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import dev.jjcoll.distributedtaskviz.repository.WorkerRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class WorkerService {
    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;
    private static final String WORKER_NAME_PREFIX = "worker-";

    public WorkerService(WorkerRepository workerRepository, WorkerMapper workerMapper) {
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
    }

    /**
     * Creates a new worker with auto-generated name if not provided.
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
     * Stops a worker by setting its status to STOPPED.
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
            return Optional.of(workerMapper.toDto(savedWorker));
        }

        return Optional.empty();
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