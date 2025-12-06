package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.exception.TaskNotFoundException;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import dev.jjcoll.distributedtaskviz.model.Worker;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.repository.TaskSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TaskService {
    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;

    public TaskService(TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.taskMapper = taskMapper;
    }


    public Task submitTask(TaskSubmissionRequestDTO newTask) {
        Task taskEntity = taskMapper.toEntity(newTask);
        return taskRepository.save(taskEntity);
    };

    public TaskResponseDTO retrieveTask(Long id) {
        Task taskEntity = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return taskMapper.toDto(taskEntity);
    };

    public  Page<TaskResponseDTO> retrieveAll(TaskStatus taskStatus, Complexity complexity, Pageable pageable) {
        // Build specification
        Specification<Task> spec = TaskSpecification.getSpecification(taskStatus, complexity);
        return taskRepository.findAll(spec, pageable).map(taskMapper::toDto);
    }

    /**
     * Claims a task for a worker. This method handles the atomic operation of:
     * 1. Finding an available PENDING task (with pessimistic lock)
     * 2. Assigning it to the worker
     * 3. Updating status to PROCESSING
     *
     * Must be @Transactional to ensure lock is held during entire operation.
     *
     * @param worker The worker claiming the task
     * @return Optional containing the claimed task, or empty if no tasks available
     */
    @Transactional
    public Optional<Task> claimTaskForWorker(Worker worker) {
        Optional<Task> taskOpt = taskRepository.findFirstPendingTaskForLocking();

        if (taskOpt.isPresent()) {
            Task task = taskOpt.get();
            task.setAssignedWorker(worker);
            task.setStatus(TaskStatus.PROCESSING);
            task.setProcessingStartedAt(LocalDateTime.now());
            taskRepository.save(task);
            logger.debug("Worker {} claimed task {}", worker.getId(), task.getId());
            return Optional.of(task);
        }

        return Optional.empty();
    }

    /**
     * Marks a task as completed successfully.
     *
     * @param taskId The ID of the task to complete
     */
    @Transactional
    public void completeTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
        logger.debug("Task {} marked as COMPLETED", taskId);
    }

    /**
     * Marks a task as failed due to processing error.
     *
     * @param taskId The ID of the task that failed
     * @param errorMessage Optional error message for logging
     */
    @Transactional
    public void failTask(Long taskId, String errorMessage) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.setStatus(TaskStatus.FAILED);
        task.setCompletedAt(LocalDateTime.now());
        taskRepository.save(task);
        logger.error("Task {} failed: {}", taskId, errorMessage);
    }
}


