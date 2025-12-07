package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.DatabaseClearResponse;
import dev.jjcoll.distributedtaskviz.model.EngineState;
import dev.jjcoll.distributedtaskviz.repository.TaskRepository;
import dev.jjcoll.distributedtaskviz.repository.WorkerRepository;
import dev.jjcoll.distributedtaskviz.simulation.WorkerSimulationManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for database operations.
 * Provides endpoints for clearing database tables.
 */
@RestController
@RequestMapping("/api/database")
@Tag(name = "Database Management", description = "APIs for database operations")
public class DatabaseController {

    private final TaskRepository taskRepository;
    private final WorkerRepository workerRepository;
    private final WorkerSimulationManager simulationManager;

    public DatabaseController(
            TaskRepository taskRepository,
            WorkerRepository workerRepository,
            WorkerSimulationManager simulationManager) {
        this.taskRepository = taskRepository;
        this.workerRepository = workerRepository;
        this.simulationManager = simulationManager;
    }

    /**
     * Clears all data from the database (tasks and workers).
     * This endpoint can only be called when the simulation engine is STOPPED.
     *
     * @return Response with details about the clearing operation
     */
    @DeleteMapping("/clear")
    @Operation(summary = "Clear all database data",
               description = "Removes all tasks and workers from the database. Can only be called when simulation is stopped.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Database cleared successfully",
                     content = @Content(schema = @Schema(implementation = DatabaseClearResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Cannot clear database: simulation is not stopped",
                     content = @Content(schema = @Schema(implementation = DatabaseClearResponse.class)))
    })
    public ResponseEntity<DatabaseClearResponse> clearDatabase() {
        EngineState currentState = simulationManager.getEngineState();

        if (currentState != EngineState.STOPPED) {
            return ResponseEntity.status(409).body(
                new DatabaseClearResponse(
                    false,
                    String.format("Cannot clear database: simulation is %s. Please stop the simulation first.", currentState),
                    0,
                    0
                )
            );
        }

        // Count before clearing
        long taskCount = taskRepository.count();
        long workerCount = workerRepository.count();

        // Clear all data
        taskRepository.deleteAll();
        workerRepository.deleteAll();

        return ResponseEntity.ok(
            new DatabaseClearResponse(
                true,
                "Database cleared successfully",
                taskCount,
                workerCount
            )
        );
    }
}
