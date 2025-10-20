package dev.jjcoll.distributedtaskviz.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers in the application.
 * HOW IT WORKS (under the hood):
 * 1. Spring uses AOP (Aspect-Oriented Programming) to wrap all @Controller/@RestController beans
 * 2. When ANY controller method throws an exception, Spring intercepts it
 * 3. Spring looks through all @ControllerAdvice beans for a matching @ExceptionHandler
 * 4. The @ExceptionHandler method signature determines which exceptions it handles
 * 5. Spring calls the matching handler method and returns its ResponseEntity
 * 6. The original exception never reaches the client - only our ErrorResponse does
 * FLOW EXAMPLE:
 * TaskController.getTask(999)
 *   → TaskService.retrieveTask(999)
 *   → throws TaskNotFoundException
 *   → Spring intercepts it
 *   → Finds handleTaskNotFoundException() below
 *   → Returns ResponseEntity<ErrorResponse> with 404 status
 *   → Client receives JSON error response
 */
@ControllerAdvice  // Tells Spring: "This class handles exceptions for ALL controllers"
public class GlobalExceptionHandler {

    /**
     * Handles TaskNotFoundException thrown anywhere in the application.
     *
     * @ExceptionHandler(TaskNotFoundException.class) tells Spring:
     *   "Call this method when TaskNotFoundException is thrown"
     * The ".class" is a Java language feature that returns the Class object
     * for TaskNotFoundException. Spring uses this to match exception types.
     *
     * @param ex The exception instance (contains the error message)
     * @param request The HTTP request that caused the error (auto-injected by Spring)
     * @return ResponseEntity with ErrorResponse body and 404 status
     */
    @ExceptionHandler(TaskNotFoundException.class)  // Match this exception type
    public ResponseEntity<ErrorResponse> handleTaskNotFoundException(
            TaskNotFoundException ex,
             HttpServletRequest request) {

        // Build standardized error response
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),    // 404
            "Not Found",                      // Status text
            ex.getMessage(),                  // "Task with id 5 not found"
            request.getRequestURI()           // "/api/tasks/5"
        );

        // Return response with 404 status
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Triggered when request body validation fails (e.g., @NotNull, @Size violations).
     * Example: POST /api/tasks with missing required fields
     * → Spring throws MethodArgumentNotValidException
     * → This handler catches it and returns 400 Bad Request
     *
     * @param ex Contains all validation errors
     * @param request The HTTP request
     * @return ResponseEntity with validation errors and 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Collect all field validation errors into a map
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();      // e.g., "name"
            String errorMessage = error.getDefaultMessage();         // e.g., "must not be null"
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),    // 400
            "Validation Failed",
            errors.toString(),                  // "{name=must not be null}"
            request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles type conversion errors in path variables or request parameters.
     * Triggered when a path variable or request parameter cannot be converted to the expected type.
     * Example: GET /api/tasks/looser (where "looser" cannot be converted to Long)
     * → Spring throws MethodArgumentTypeMismatchException
     * → This handler catches it and returns 400 Bad Request
     *
     * @param ex The MethodArgumentTypeMismatchException instance
     * @param request The HTTP request with invalid parameter type
     * @return ResponseEntity with error details and 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {

        String parameterName = ex.getName();
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";

        String message = String.format(
                "Invalid value '%s' for parameter '%s'. Expected type: %s",
                invalidValue,
                parameterName,
                requiredType
        );

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),    // 400
                "Bad Request",
                message,
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles NoResourceFoundException when client hits an endpoint that does not exist.
     * Triggered when Spring cannot find a matching controller method for the request.
     * Example: GET /api/nonexistent
     * → Spring throws NoResourceFoundException
     * → This handler catches it and returns 404 Not Found with descriptive message
     *
     * @param ex The NoResourceFoundException instance
     * @param request The HTTP request that hit the non-existent endpoint
     * @return ResponseEntity with error details and 404 status
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            NoResourceFoundException ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "No endpoint found for " + request.getMethod() + " " + request.getRequestURI(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Catch-all handler for any exception not handled by specific handlers above.
     * IMPORTANT: This must be LAST because @ExceptionHandler matches from most specific
     * to least specific. Since Exception.class is the parent of all exceptions,
     * it acts as a fallback.
     * Example: NullPointerException, IllegalArgumentException, etc.
     * → This handler catches them and returns 500
     *
     * @param ex Any unhandled exception
     * @param request The HTTP request
     * @return ResponseEntity with generic error and 500 status
     */
    @ExceptionHandler(Exception.class)  // Catches ALL exceptions (fallback)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),    // 500
            "Internal Server Error",
            ex.getMessage(),
            request.getRequestURI()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }


}
