package dev.jjcoll.distributedtaskviz.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception for when a Task resource is not found.
 *
 * @ResponseStatus tells Spring what HTTP status to return if this exception
 * is NOT caught by a @ControllerAdvice. Since we have GlobalExceptionHandler,
 * that takes precedence, but this serves as a fallback and documentation.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Task not found")
public class TaskNotFoundException extends RuntimeException {

    /**
     * Constructor with custom message.
     * Calls super(message) to set the exception's message field.
     *
     * @param message Custom error message
     */
    public TaskNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor that auto-formats the error message with the task ID.
     *
     * Example: new TaskNotFoundException(5L)
     * → message becomes "Task with id 5 not found"
     *
     * @param taskId The ID of the task that wasn't found
     */
    public TaskNotFoundException(Long taskId) {
        super("Task with id " + taskId + " not found");
    }
}