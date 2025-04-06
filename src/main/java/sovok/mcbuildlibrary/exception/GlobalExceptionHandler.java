// file: src/main/java/sovok/mcbuildlibrary/exception/GlobalExceptionHandler.java
package sovok.mcbuildlibrary.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.lang.IllegalArgumentException;
import java.lang.IllegalStateException; // Import
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException; // Import
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

    // --- 400 Bad Request Handlers ---

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            String parameterName = path.substring(path.lastIndexOf('.') + 1);
            errors.put(parameterName, violation.getMessage());
        }
        log.warn("{}: {}", StringConstants.VALIDATION_FAILED_MESSAGE, errors, ex);
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
        log.warn("{}: {}", StringConstants.VALIDATION_FAILED_MESSAGE, errors, ex);
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
        log.warn("{}: {}", StringConstants.INPUT_ERROR_MESSAGE, errors, ex);
        return new ValidationErrorResponse(HttpStatus.BAD_REQUEST, StringConstants.INPUT_ERROR_MESSAGE, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                String.format(StringConstants.MISSING_PARAMETER_MESSAGE, ex.getParameterName(), ex.getParameterType()));
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Parameter '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE, ex.getParameterName(), ex);
        return pd;
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestPartException(MissingServletRequestPartException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                String.format(StringConstants.MISSING_FILE_PART_MESSAGE, ex.getRequestPartName()));
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: Required part '{}' is missing", StringConstants.INPUT_ERROR_MESSAGE, ex.getRequestPartName(), ex);
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        // ... (handler remains the same) ...
        String paramName = ex.getName();
        Object invalidValue = ex.getValue();
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String detail = String.format(StringConstants.TYPE_MISMATCH_MESSAGE, invalidValue, paramName, expectedType);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE);
        log.warn("{}: {}", StringConstants.INPUT_ERROR_MESSAGE, detail, ex);
        return pd;
    }

    // Catch IllegalArgumentException for various bad inputs (duplicate name, invalid query param, empty file)
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // Keep 400 for these cases
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle(StringConstants.INPUT_ERROR_MESSAGE); // General input error title
        // Distinguish logging slightly if needed, but message comes from where it was thrown
        log.warn("{}: {}", StringConstants.INPUT_ERROR_MESSAGE, ex.getMessage()); // Removed stack trace logging for client errors
        return pd;
    }

    // --- 404 Not Found Handler ---
    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNoSuchElementException(NoSuchElementException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE);
        log.warn("{}: {}", StringConstants.NOT_FOUND_MESSAGE, ex.getMessage()); // Log message, no stack trace for not found
        return pd;
    }

    // Existing handler for NoResourceFoundException (e.g., invalid static resource path)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
            NoResourceFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle(StringConstants.NOT_FOUND_MESSAGE);
        log.warn("{}: {}", StringConstants.NOT_FOUND_MESSAGE, ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }


    // --- 409 Conflict Handler ---
    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleIllegalStateException(IllegalStateException ex) {
        // Assuming IllegalStateException is used primarily for deletion conflicts based on our service changes
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict"); // More specific title for 409
        log.warn("Conflict: {}", ex.getMessage()); // Log message, no stack trace for conflict
        return pd;
    }


    // --- Generic Fallback Handler (500) ---
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ProblemDetail handleGenericException(Exception ex) {
        // Update check to include new standard exceptions handled above
        if (ex instanceof MissingServletRequestPartException ||
                ex instanceof MissingServletRequestParameterException ||
                ex instanceof IllegalArgumentException ||
                ex instanceof ConstraintViolationException ||
                ex instanceof MethodArgumentNotValidException ||
                ex instanceof MethodArgumentTypeMismatchException ||
                ex instanceof BindException ||
                ex instanceof NoSuchElementException || // Added
                ex instanceof IllegalStateException) { // Added
            log.warn("Exception handled by generic handler but should have been caught earlier: {}", ex.getMessage());
        } else {
            log.error("Unhandled exception: {}", ex.getMessage(), ex);
        }
        String message = "An unexpected error occurred.";
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, message);
        pd.setTitle("Internal Server Error");
        return pd;
    }
}