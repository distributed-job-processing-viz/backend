package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.WorkerCreateRequestDTO;
import dev.jjcoll.distributedtaskviz.dto.WorkerResponseDTO;
import dev.jjcoll.distributedtaskviz.service.WorkerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
@Tag(name = "Worker Management", description = "APIs for managing worker instances in the distributed system")
public class WorkerController {

    private final WorkerService workerService;

    public WorkerController(WorkerService workerService) {
        this.workerService = workerService;
    }

    @Operation(
            summary = "Create a new worker",
            description = "Creates a new worker instance. If name is not provided, it will be auto-generated (e.g., 'worker-1', 'worker-2')"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Worker created successfully",
                    content = @Content(schema = @Schema(implementation = WorkerResponseDTO.class))
            )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkerResponseDTO createWorker(
            @Parameter(description = "Optional worker creation request. If name is not provided, a name will be auto-generated")
            @RequestBody(required = false) WorkerCreateRequestDTO request
    ) {
        WorkerCreateRequestDTO createRequest = request != null ? request : new WorkerCreateRequestDTO(null);
        return workerService.createWorker(createRequest);
    }

    @Operation(
            summary = "Get a worker by ID",
            description = "Retrieves details of a specific worker by its ID"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Worker found",
                    content = @Content(schema = @Schema(implementation = WorkerResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Worker not found"
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<WorkerResponseDTO> getWorker(
            @Parameter(description = "ID of the worker to retrieve")
            @PathVariable Long id
    ) {
        return workerService.getWorker(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Get all workers",
            description = "Retrieves a list of all worker instances in the system"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "List of workers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = WorkerResponseDTO.class))
            )
    })
    @GetMapping
    public List<WorkerResponseDTO> getAllWorkers() {
        return workerService.getAllWorkers();
    }

    @Operation(
            summary = "Stop a worker",
            description = "Stops a worker by setting its status to STOPPED"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Worker stopped successfully",
                    content = @Content(schema = @Schema(implementation = WorkerResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Worker not found"
            )
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<WorkerResponseDTO> stopWorker(
            @Parameter(description = "ID of the worker to stop")
            @PathVariable Long id
    ) {
        return workerService.stopWorker(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}