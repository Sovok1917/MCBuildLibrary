package sovok.mcbuildlibrary.exception;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ValidationErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error, // General error type (e.g., "Bad Request")
        String message, // Overall message (e.g., "Validation Failed")
        Map<String, String> details // Field-specific errors
) {
    public ValidationErrorResponse(HttpStatus status, String message, Map<String, String> details) {
        this(LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);
    }
}