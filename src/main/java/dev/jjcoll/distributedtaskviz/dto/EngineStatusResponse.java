package dev.jjcoll.distributedtaskviz.dto;

import dev.jjcoll.distributedtaskviz.model.EngineState;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO containing task processing engine status information.
 */
@Schema(description = "Task processing engine status information")
public record EngineStatusResponse(
        @Schema(description = "Current engine state", example = "RUNNING")
        EngineState state,

        @Schema(description = "Status message", example = "Engine started successfully")
        String message,

        @Schema(description = "Number of active workers currently processing tasks", example = "5", nullable = true)
        Integer activeWorkerCount
) {
    /**
     * Constructor for responses without active worker count.
     */
    public EngineStatusResponse(EngineState state, String message) {
        this(state, message, null);
    }
}
