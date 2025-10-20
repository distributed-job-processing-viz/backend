package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
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
}
