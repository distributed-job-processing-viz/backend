package dev.jjcoll.distributedtaskviz.dto;

import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for Task responses.
 * This record is used to send task data to API clients.
 * Records are immutable, providing thread-safety and automatic generation of:
 * - Constructor with all fields
 * - Getters (accessor methods)
 * - equals(), hashCode(), and toString()
 */
@Schema(description = "Response object containing task information")
public record TaskResponseDTO(
        @Schema(description = "Unique identifier of the task", example = "1")
        Long id,

        @Schema(description = "Name/description of the task", example = "Process user data")
        String name,

        @Schema(description = "Complexity of the task, implicitly related to time task takes to complete", example = "HIGH")
        Complexity complexity,

        @Schema(description = "Current status of the task", example = "PENDING")
        TaskStatus status,

        @Schema(description = "Timestamp when the task was created", example = "2025-10-08T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp when the task was completed (null if not completed)", example = "2025-10-08T14:35:00", nullable = true)
        LocalDateTime completedAt
) {}
