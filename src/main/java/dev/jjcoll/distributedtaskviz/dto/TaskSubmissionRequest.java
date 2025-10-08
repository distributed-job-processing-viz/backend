package dev.jjcoll.distributedtaskviz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Data Transfer Object (DTO) for task submission requests.
 * This POJO is used to receive task data from API clients when creating a new task.
 * It only contains the information needed to create a task (not the ID or timestamps).
 */
@Schema(description = "Request object for submitting a new task")
public class TaskSubmissionRequest {

    /**
     * The name or description of the task to be created.
     * This field is required and cannot be blank.
     * The @NotBlank annotation ensures validation at the API level.
     */
    @Schema(description = "Name/description of the task to create", example = "Process user data", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Task name is required and cannot be blank")
    private String name;

    // Default constructor
    public TaskSubmissionRequest() {
    }

    // Constructor with name
    public TaskSubmissionRequest(String name) {
        this.name = name;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
