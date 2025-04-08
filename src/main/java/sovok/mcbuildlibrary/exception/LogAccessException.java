package sovok.mcbuildlibrary.exception;

/**
 * Custom runtime exception for errors specifically related to accessing
 * or processing application log files (e.g., reading directories, reading files).
 * Extends RuntimeException, making it an unchecked exception.
 */
public class LogAccessException extends RuntimeException {

    /**
     * Constructs a new log access exception with the specified detail message and cause.
     * Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this runtime exception's detail message.</p>
     *
     * @param message the detail message (which is saved for later retrieval by the
     *                {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method). (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public LogAccessException(String message, Throwable cause) {
        // Pass the message and cause to the RuntimeException superclass constructor.
        // This is where the parameters are "used", satisfying IntelliJ.
        super(message, cause);
    }

}