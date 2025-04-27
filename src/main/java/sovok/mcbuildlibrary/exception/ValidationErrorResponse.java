package sovok.mcbuildlibrary.exception;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ValidationErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {
    public ValidationErrorResponse(HttpStatus status, String message, Map<String, String> details) {
        this(LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details);
    }
}