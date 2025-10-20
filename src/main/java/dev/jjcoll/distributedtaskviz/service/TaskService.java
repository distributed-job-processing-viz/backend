package dev.jjcoll.distributedtaskviz.service;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.exception.TaskNotFoundException;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
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
}


