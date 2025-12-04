package dev.jjcoll.distributedtaskviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object (DTO) for worker creation requests.
 * This record is used to receive worker data from API clients when creating a new worker.
 * The name field is optional - if not provided, it will be auto-generated (e.g., "worker-1", "worker-2").
 * Records provide immutability and automatic generation of constructor, getters, equals(), hashCode(), and toString().
 */
@Schema(description = "Request object for creating a new worker")
public record WorkerCreateRequestDTO(
        @Schema(description = "Optional name of the worker. If not provided, will be auto-generated", example = "worker-1", nullable = true)
        String name
) {}