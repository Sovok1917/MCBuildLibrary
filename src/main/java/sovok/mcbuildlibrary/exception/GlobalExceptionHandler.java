package sovok.mcbuildlibrary.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for the application.
 * Catches specific exceptions and returns standardized error responses (ProblemDetail or
 * ValidationErrorResponse).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles bean validation constraint violations.
     *
     * @param ex The ConstraintViolationException thrown.
     * @return A ValidationErrorResponse containing details of the violations.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolationException(
            ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            // Extract the simple parameter name from the full path
            String parameterName = path;
            int lastDot = path.lastIndexOf('.');
            if (lastDot != -1 && lastDot < path.length() - 1) {
                parameterName = path.substring(lastDot + 1);
            }
            errors.put(parameterName, violation.getMessage());
        }
        log.warn(StringConstants.LOG_MESSAGE_FORMAT,
                StringConstants.VALIDATION_FAILED_MESSAGE, errors);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST,
                StringConstants.VALIDATION_FAILED_MESSAGE, errors);
    }

    /**
     * Handles validation errors on @RequestBody objects annotated with @Valid.
     *
     * @param ex The MethodArgumentNotValidException thrown.
     * @return A ValidationErrorResponse containing details of the field errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn(StringConstants.LOG_MESSAGE_FORMAT,
                StringConstants.VALIDATION_FAILED_MESSAGE, errors);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST,
                StringConstants.VALIDATION_FAILED_MESSAGE, errors);
    }

    /**
     * Handles data binding errors (e.g., type mismatches in form data).
     *
     * @param ex The BindException thrown.
     * @return A ValidationErrorResponse containing details of the binding errors.
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.INPUT_ERROR_MESSAGE, errors);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST,
                StringConstants.INPUT_ERROR_MESSAGE, errors);
    }

    /**
     * Handles missing required request parameters.
     *
     * @param ex The MissingServletRequestParameterException thrown.
     * @return A ProblemDetail describing the missing parameter.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        String detail = String.format(StringConstants.MISSING_PARAMETER_MESSAGE,
                ex.getParameterName(), ex.getParameterType());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Parameter '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE,
                ex.getParameterName());
        return pd;
    }

    /**
     * Handles missing required parts in multipart requests (e.g., file uploads).
     *
     * @param ex The MissingServletRequestPartException thrown.
     * @return A ProblemDetail describing the missing part.
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestPartException(
            MissingServletRequestPartException ex) {
        String detail = String.format(StringConstants.MISSING_FILE_PART_MESSAGE,
                ex.getRequestPartName());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Required part '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE,
                ex.getRequestPartName());
        return pd;
    }

    /**
     * Handles type mismatch errors for request parameters or path variables.
     *
     * @param ex The MethodArgumentTypeMismatchException thrown.
     * @return A ProblemDetail describing the type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object invalidValue = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();
        String expectedType = (requiredType != null) ? requiredType.getSimpleName() : "unknown";

        if (requiredType == null) {
            log.warn("Could not determine required type for parameter '{}'. Value provided: '{}'",
                    paramName, invalidValue);
        }

        String detail = String.format(StringConstants.TYPE_MISMATCH_MESSAGE,
                invalidValue, paramName, expectedType);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.INPUT_ERROR_MESSAGE, detail);
        return pd;
    }

    /**
     * Handles illegal argument exceptions, often used for semantic validation errors.
     * Includes specific handling for invalid Task ID format messages.
     *
     * @param ex The IllegalArgumentException thrown.
     * @return A ProblemDetail describing the error.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex.getMessage());
        // Set a more specific title if the message indicates the invalid Task ID format
        if (ex.getMessage() != null
                && ex.getMessage().equals(StringConstants.INVALID_TASK_ID_FORMAT)) {
            pd.setTitle(StringConstants.INVALID_TASK_ID_FORMAT);
        } else {
            pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE); // Default title
        }
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, pd.getTitle(), ex.getMessage());
        return pd;
    }

    /**
     * Handles cases where a requested resource or element is not found.
     * Used for Build not found, Task ID not found, Log file not found, Task failed scenarios.
     *
     * @param ex The NoSuchElementException thrown.
     * @return A ProblemDetail indicating the resource was not found.
     */
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE); // Keep generic "Not Found" title
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.NOT_FOUND_MESSAGE,
                ex.getMessage());
        return pd;
    }

    /**
     * Handles requests for non-existent resource paths within the application context.
     *
     * @param ex The NoResourceFoundException thrown.
     * @return A ResponseEntity with status 404 and a ProblemDetail.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
            NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE);
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.NOT_FOUND_MESSAGE,
                ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    /**
     * Handles illegal state exceptions, often indicating an operation cannot be performed
     * due to the current state (e.g., deleting an entity with active associations).
     *
     * @param ex The IllegalStateException thrown.
     * @return A ProblemDetail indicating a conflict.
     */
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        log.warn("Conflict: {}", ex.getMessage());
        return pd;
    }

    /**
     * Handles exceptions specific to accessing application log files (not build logs).
     *
     * @param ex The LogAccessException thrown.
     * @return A ProblemDetail indicating an internal server error related to log access.
     */
    @ExceptionHandler(LogAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleLogAccessException(LogAccessException ex) {
        String cause = (ex.getCause() != null) ? ex.getCause().getClass().getSimpleName() : "N/A";
        log.error("Log access error: {} - Cause: {}", ex.getMessage(), cause, ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal error occurred while accessing application logs.");
        pd.setTitle("Log Access Error");
        return pd;
    }

    /**
     * Generic fallback handler for any other uncaught exceptions.
     *
     * @param ex The Exception thrown.
     * @return A ProblemDetail indicating a generic internal server error.
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(Exception ex) {
        // Check if it's one of the exceptions that should have been caught by more
        // specific handlers
        if (ex instanceof MissingServletRequestPartException
                || ex instanceof MissingServletRequestParameterException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof BindException
                || ex instanceof NoSuchElementException
                || ex instanceof IllegalStateException
                || ex instanceof LogAccessException) {
            log.warn("Exception handled by generic handler but should have been caught earlier:"
                    + " {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            // Log truly unexpected exceptions as errors
            log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        }

        String message = "An unexpected internal error occurred. Please contact support.";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                message);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}