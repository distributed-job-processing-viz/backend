package dev.jjcoll.distributedtaskviz.exception;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Standardized error response structure for all API errors.
 * This ensures consistent error format across the entire API.
 *
 * Example JSON response:
 * {
 *   "timestamp": "2025-10-08T14:30:00",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Task with id 5 not found",
 *   "path": "/api/tasks/5"
 * }
 */
@Getter  // Lombok: generates getters for all fields (needed for JSON serialization)
@Setter  // Lombok: generates setters for all fields
@NoArgsConstructor  // Lombok: generates a no-argument constructor (required by Jackson for deserialization)
public class ErrorResponse {

    // Field initializer: sets timestamp automatically when object is created
    private LocalDateTime timestamp = LocalDateTime.now();

    private int status;        // HTTP status code (e.g., 404, 500)
    private String error;      // HTTP status text (e.g., "Not Found", "Internal Server Error")
    private String message;    // Detailed error message
    private String path;       // The request URI that caused the error

    /**
     * All-args constructor (except timestamp, which is auto-set).
     * Used by GlobalExceptionHandler to create error responses.
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();  // Always set current time
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}