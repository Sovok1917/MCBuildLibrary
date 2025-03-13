package sovok.mcbuildlibrary.exception;

public class InvalidQueryParameterException extends RuntimeException {
    public InvalidQueryParameterException(String param) {
        super("Invalid query parameter: " + param
                + ". Allowed parameters are: author, name, theme, color.");
    }
}