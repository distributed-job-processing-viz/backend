package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.exception.TaskNotFoundException;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.repository.TaskSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TaskService {
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
}


