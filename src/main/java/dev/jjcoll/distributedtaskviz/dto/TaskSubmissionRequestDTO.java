package dev.jjcoll.distributedtaskviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for task submission requests.
 * This record is used to receive task data from API clients when creating a new task.
 * It only contains the information needed to create a task (not the ID or timestamps).
 * Records provide immutability and automatic generation of constructor, getters, equals(), hashCode(), and toString().
 */
@Schema(description = "Request object for submitting a new task")
public record TaskSubmissionRequestDTO(
        /**
         * The name or description of the task to be created.
         * This field is required and cannot be blank.
         * The @NotBlank annotation ensures validation at the API level.
         */
        @Schema(description = "Name/description of the task to create", example = "Process user data", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Task name is required and cannot be blank")
        String name
) {}
