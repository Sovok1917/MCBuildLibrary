package sovok.mcbuildlibrary.exception;

public class InvalidQueryParameterException extends RuntimeException {

    // Constructor updated to use String.format and the constant
    public InvalidQueryParameterException(String invalidParamName) {
        super(String.format(StringConstants.INVALID_QUERY_PARAMETER, invalidParamName));
    }
}