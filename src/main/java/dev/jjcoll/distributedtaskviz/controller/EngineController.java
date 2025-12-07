package dev.jjcoll.distributedtaskviz.controller;

import dev.jjcoll.distributedtaskviz.dto.EngineStatusResponse;
import dev.jjcoll.distributedtaskviz.model.EngineState;
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
 * REST controller for managing the task processing engine lifecycle.
 * Provides endpoints to start, stop, and check the status of the processing engine.
 */
@RestController
@RequestMapping("/api/engine")
@Tag(name = "Engine Control", description = "APIs for controlling the task processing engine")
public class EngineController {

    private final WorkerSimulationManager simulationManager;

    public EngineController(WorkerSimulationManager simulationManager) {
        this.simulationManager = simulationManager;
    }

    /**
     * Starts the task processing engine. All pending workers are activated
     * and begin processing tasks.
     *
     * @return Engine status response with activation details
     */
    @PostMapping("/start")
    @Operation(summary = "Start the processing engine",
               description = "Activates all pending workers to begin processing tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Engine started successfully",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Engine is already running",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class)))
    })
    public ResponseEntity<EngineStatusResponse> startEngine() {
        EngineState currentState = simulationManager.getEngineState();

        if (currentState == EngineState.RUNNING) {
            return ResponseEntity.status(409).body(
                new EngineStatusResponse(
                    EngineState.RUNNING,
                    "Engine is already running",
                    simulationManager.getActiveWorkerCount()
                )
            );
        }

        int activatedCount = simulationManager.startEngine();
        int activeWorkerCount = simulationManager.getActiveWorkerCount();

        String message;
        if (activatedCount > 0) {
            // Cold start: workers were pending and got activated
            message = String.format("Engine started successfully. Activated %d workers", activatedCount);
        } else if (activeWorkerCount > 0) {
            // Resume: engine was paused, workers already active
            message = String.format("Engine resumed successfully. %d workers active", activeWorkerCount);
        } else {
            // Edge case: no workers at all
            message = "Engine started successfully. No workers available";
        }

        return ResponseEntity.ok(
            new EngineStatusResponse(
                EngineState.RUNNING,
                message,
                activeWorkerCount
            )
        );
    }

    /**
     * Stops the task processing engine. All active workers are stopped gracefully.
     * In-flight tasks will be marked as FAILED.
     *
     * @return Engine status response with deactivation details
     */
    @PostMapping("/stop")
    @Operation(summary = "Stop the processing engine",
               description = "Gracefully stops all active workers. In-flight tasks will be marked as FAILED")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Engine stopped successfully",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Engine is already stopped",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class)))
    })
    public ResponseEntity<EngineStatusResponse> stopEngine() {
        EngineState currentState = simulationManager.getEngineState();

        if (currentState == EngineState.STOPPED) {
            return ResponseEntity.status(409).body(
                new EngineStatusResponse(
                    EngineState.STOPPED,
                    "Engine is already stopped",
                    0
                )
            );
        }

        int activeCount = simulationManager.getActiveWorkerCount();
        simulationManager.stopEngine();

        return ResponseEntity.ok(
            new EngineStatusResponse(
                EngineState.STOPPED,
                String.format("Engine stopped successfully. Deactivated %d workers", activeCount),
                0
            )
        );
    }

    /**
     * Pauses the task processing engine. Worker threads remain alive but idle.
     * Tasks are not processed until engine is resumed.
     *
     * @return Engine status response with pause details
     */
    @PostMapping("/pause")
    @Operation(summary = "Pause the processing engine",
               description = "Pauses all workers. Threads remain alive but idle. Tasks not processed.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Engine paused successfully",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Engine is already paused or stopped",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class)))
    })
    public ResponseEntity<EngineStatusResponse> pauseEngine() {
        EngineState currentState = simulationManager.getEngineState();

        if (currentState == EngineState.PAUSED) {
            return ResponseEntity.status(409).body(
                new EngineStatusResponse(
                    EngineState.PAUSED,
                    "Engine is already paused",
                    simulationManager.getActiveWorkerCount()
                )
            );
        }

        if (currentState == EngineState.STOPPED) {
            return ResponseEntity.status(409).body(
                new EngineStatusResponse(
                    EngineState.STOPPED,
                    "Cannot pause: engine is stopped",
                    0
                )
            );
        }

        int activeCount = simulationManager.getActiveWorkerCount();
        simulationManager.pauseEngine();

        return ResponseEntity.ok(
            new EngineStatusResponse(
                EngineState.PAUSED,
                String.format("Engine paused successfully. %d workers idle", activeCount),
                activeCount
            )
        );
    }

    /**
     * Resumes the task processing engine from paused state.
     * Workers continue processing tasks.
     *
     * @return Engine status response with resume details
     */
    @PostMapping("/resume")
    @Operation(summary = "Resume the processing engine",
               description = "Resumes workers from paused state to continue processing tasks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Engine resumed successfully",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class))),
        @ApiResponse(responseCode = "409",
                     description = "Engine is not paused",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class)))
    })
    public ResponseEntity<EngineStatusResponse> resumeEngine() {
        EngineState currentState = simulationManager.getEngineState();

        if (currentState != EngineState.PAUSED) {
            return ResponseEntity.status(409).body(
                new EngineStatusResponse(
                    currentState,
                    String.format("Cannot resume: engine is %s (not paused)", currentState),
                    simulationManager.getActiveWorkerCount()
                )
            );
        }

        int activeCount = simulationManager.getActiveWorkerCount();
        simulationManager.resumeEngine();

        return ResponseEntity.ok(
            new EngineStatusResponse(
                EngineState.RUNNING,
                String.format("Engine resumed successfully. %d workers active", activeCount),
                activeCount
            )
        );
    }

    /**
     * Gets the current status of the task processing engine.
     *
     * @return Current engine state and worker counts
     */
    @GetMapping("/status")
    @Operation(summary = "Get engine status",
               description = "Returns current engine state and active worker count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200",
                     description = "Engine status retrieved successfully",
                     content = @Content(schema = @Schema(implementation = EngineStatusResponse.class)))
    })
    public ResponseEntity<EngineStatusResponse> getEngineStatus() {
        EngineState state = simulationManager.getEngineState();
        int activeWorkers = simulationManager.getActiveWorkerCount();
        int pendingWorkers = simulationManager.getPendingWorkerCount();

        String message;
        if (state == EngineState.RUNNING) {
            message = String.format("Engine is running with %d active workers", activeWorkers);
        } else if (state == EngineState.PAUSED) {
            message = String.format("Engine is paused with %d idle workers", activeWorkers);
        } else {
            message = String.format("Engine is stopped with %d pending workers", pendingWorkers);
        }

        return ResponseEntity.ok(
            new EngineStatusResponse(state, message, activeWorkers)
        );
    }
}
