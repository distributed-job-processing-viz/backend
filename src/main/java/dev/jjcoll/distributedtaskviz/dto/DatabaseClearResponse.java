package dev.jjcoll.distributedtaskviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for database clear operation.
 */
@Schema(description = "Database clear operation response")
public record DatabaseClearResponse(
        @Schema(description = "Whether the operation was successful", example = "true")
        boolean success,

        @Schema(description = "Status message", example = "Database cleared successfully")
        String message,

        @Schema(description = "Number of tasks deleted", example = "42")
        long tasksDeleted,

        @Schema(description = "Number of workers deleted", example = "5")
        long workersDeleted
) {
}
