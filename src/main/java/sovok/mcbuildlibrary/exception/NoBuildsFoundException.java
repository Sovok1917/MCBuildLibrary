package sovok.mcbuildlibrary.exception;

public class NoBuildsFoundException extends RuntimeException {
    public NoBuildsFoundException(String message) {
        super(message);
    }

    public NoBuildsFoundException() {
        super("No builds were found matching the provided query parameters.");
    }
}
