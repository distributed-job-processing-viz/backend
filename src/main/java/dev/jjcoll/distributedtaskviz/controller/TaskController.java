package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService, TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskService = taskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task submitTask(@Valid @RequestBody TaskSubmissionRequestDTO request) {
        return taskService.submitTask(request);
    }

    @GetMapping("/{id}")
    public TaskResponseDTO getTask(@PathVariable Long id) {
        return taskService.retrieveTask(id);
    }

    @GetMapping
    public Page<TaskResponseDTO> getAllTasks(
            @RequestParam(name = "status", required = false) TaskStatus taskStatus,
            @RequestParam(name = "complexity", required = false) Complexity complexity,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
            ) {
            return taskService.retrieveAll(taskStatus, complexity, pageable);
    }
}
