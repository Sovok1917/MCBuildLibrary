// file: src/main/java/sovok/mcbuildlibrary/exception/GlobalExceptionHandler.java
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

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();

            String parameterName = path;
            int lastDot = path.lastIndexOf('.');
            if (lastDot != -1 && lastDot < path.length() - 1) {
                parameterName = path.substring(lastDot + 1);
            }
            errors.put(parameterName, violation.getMessage());
        }
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.VALIDATION_FAILED_MESSAGE, errors);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST, StringConstants.VALIDATION_FAILED_MESSAGE, errors);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.VALIDATION_FAILED_MESSAGE, errors);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST, StringConstants.VALIDATION_FAILED_MESSAGE, errors);
    }

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
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST, StringConstants.INPUT_ERROR_MESSAGE, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                String.format(StringConstants.MISSING_PARAMETER_MESSAGE, ex.getParameterName(), ex.getParameterType()));
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Parameter '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE, ex.getParameterName());
        return pd;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                String.format(StringConstants.MISSING_FILE_PART_MESSAGE, ex.getRequestPartName()));
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Required part '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE, ex.getRequestPartName());
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        Object invalidValue = ex.getValue();

        Class<?> requiredType = ex.getRequiredType();
        String expectedType;
        if (requiredType != null) {
            expectedType = requiredType.getSimpleName();
        } else {
            expectedType = "unknown";
            log.warn("Could not determine required type for parameter '{}'. Value provided: '{}'", paramName, invalidValue);
        }

        String detail = String.format(StringConstants.TYPE_MISMATCH_MESSAGE, invalidValue, paramName, expectedType);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.INPUT_ERROR_MESSAGE, detail);
        return pd;
    }

    // Handles invalid task ID format and other general bad arguments
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        // Set a more specific title if the message indicates the invalid Task ID format
        if (ex.getMessage() != null && ex.getMessage().equals(StringConstants.INVALID_TASK_ID_FORMAT)) {
            pd.setTitle(StringConstants.INVALID_TASK_ID_FORMAT);
        } else {
            pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE); // Default title
        }
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, pd.getTitle(), ex.getMessage());
        return pd;
    }

    // Handles Build not found, Task ID not found, Log file not found (after completion), Task failed
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE); // Keep generic "Not Found" title
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.NOT_FOUND_MESSAGE, ex.getMessage());
        return pd;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE);
        log.warn(StringConstants.LOG_MESSAGE_FORMAT, StringConstants.NOT_FOUND_MESSAGE, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    // Handles conflicts like trying to delete associated entities
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        log.warn("Conflict: {}", ex.getMessage());
        return pd;
    }

    // Handles errors specific to LogController accessing general application logs
    @ExceptionHandler(LogAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleLogAccessException(LogAccessException ex) {
        log.error("Log access error: {} - Cause: {}", ex.getMessage(), ex.getCause() != null ? ex.getCause().getClass().getSimpleName() : "N/A", ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred while accessing application logs.");
        pd.setTitle("Log Access Error");
        return pd;
    }

    // Generic fallback for unexpected errors
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(Exception ex) {
        // Check if it's one of the exceptions that should have been caught by more specific handlers
        if (ex instanceof MissingServletRequestPartException
                || ex instanceof MissingServletRequestParameterException
                || ex instanceof IllegalArgumentException
                || ex instanceof ConstraintViolationException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof BindException
                || ex instanceof NoSuchElementException
                || ex instanceof IllegalStateException
                || ex instanceof LogAccessException) {
            log.warn("Exception handled by generic handler but should have been caught earlier: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        } else {
            // Log truly unexpected exceptions as errors
            log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        }

        String message = "An unexpected internal error occurred. Please contact support.";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}