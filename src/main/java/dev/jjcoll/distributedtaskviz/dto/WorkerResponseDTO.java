package dev.jjcoll.distributedtaskviz.dto;

import dev.jjcoll.distributedtaskviz.model.WorkerStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for Worker responses.
 * This record is used to send worker data to API clients.
 * Records are immutable, providing thread-safety and automatic generation of:
 * - Constructor with all fields
 * - Getters (accessor methods)
 * - equals(), hashCode(), and toString()
 */
@Schema(description = "Response object containing worker information")
public record WorkerResponseDTO(
        @Schema(description = "Unique identifier of the worker", example = "1")
        Long id,

        @Schema(description = "Name of the worker", example = "worker-1")
        String name,

        @Schema(description = "Current status of the worker", example = "IDLE")
        WorkerStatus status,

        @Schema(description = "Timestamp when the worker was created", example = "2025-12-04T14:30:00")
        LocalDateTime createdAt,

        @Schema(description = "Timestamp of the last heartbeat from the worker", example = "2025-12-04T14:35:00", nullable = true)
        LocalDateTime lastHeartbeat
) {}