package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.TaskResponseDTO;
import dev.jjcoll.distributedtaskviz.dto.TaskSubmissionRequestDTO;
import dev.jjcoll.distributedtaskviz.mappers.TaskMapper;
import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Task Management", description = "APIs for managing tasks in the distributed task processing system")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService, TaskRepository taskRepository, TaskMapper taskMapper) {
        this.taskService = taskService;
    }

    @Operation(
            summary = "Submit a new task",
            description = "Submits a new task to the distributed task processing system for execution"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Task submitted successfully",
                    content = @Content(schema = @Schema(implementation = Task.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid task request"
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Task submitTask(
            @Parameter(description = "Task submission request containing task details")
            @Valid @RequestBody TaskSubmissionRequestDTO request
    ) {
        return taskService.submitTask(request);
    }

    @Operation(
            summary = "Get a task by ID",
            description = "Retrieves details of a specific task by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Task found",
                    content = @Content(schema = @Schema(implementation = TaskResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Task not found"
            )
    })
    @GetMapping("/{id}")
    public TaskResponseDTO getTask(
            @Parameter(description = "ID of the task to retrieve")
            @PathVariable Long id
    ) {
        return taskService.retrieveTask(id);
    }

    @Operation(
            summary = "Get all tasks",
            description = "Retrieves a paginated list of tasks with optional filtering by status and complexity. Results are sorted by creation date in descending order by default."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of tasks retrieved successfully",
                    content = @Content(schema = @Schema(implementation = Page.class))
            )
    })
    @GetMapping
    public Page<TaskResponseDTO> getAllTasks(
            @Parameter(description = "Filter tasks by status (e.g., PENDING, RUNNING, COMPLETED)")
            @RequestParam(name = "status", required = false) TaskStatus taskStatus,
            @Parameter(description = "Filter tasks by complexity level (e.g., LOW, MEDIUM, HIGH)")
            @RequestParam(name = "complexity", required = false) Complexity complexity,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
            ) {
            return taskService.retrieveAll(taskStatus, complexity, pageable);
    }
}
