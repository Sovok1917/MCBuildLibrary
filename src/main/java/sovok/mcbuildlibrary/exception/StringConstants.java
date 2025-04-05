package sovok.mcbuildlibrary.exception;

public final class StringConstants { // Make final and add private constructor

    private StringConstants() {
        // Private constructor to prevent instantiation
    }

    // --- General ---
    public static final String NOT_FOUND_MESSAGE = "not found";
    public static final String ALREADY_EXISTS_MESSAGE = "already exists";
    public static final String CANNOT_DELETE_ASSOCIATED = "Cannot delete %s '%s' because "
            + "it is associated with %d build(s)."; // %s=entity type, %s=name, %d=count
    public static final String QUERY_NO_RESULTS = "No %s found matching the query: %s";
    // %s=entity type plural, %s=query details
    public static final String NO_ENTITIES_AVAILABLE = "No %s are currently available";
    // %s=entity type plural

    // --- Resource Identifiers ---
    public static final String WITH_ID = "with ID";
    public static final String WITH_NAME = "with name";

    // --- Entity Specific ---
    public static final String AUTHOR = "Author";
    public static final String BUILD = "Build";
    public static final String COLOR = "Color";
    public static final String THEME = "Theme";
    public static final String SCHEM_FILE = "Schem file";

    // --- Resource Specific Messages ---
    public static final String RESOURCE_NOT_FOUND_TEMPLATE = "%s %s '%s' %s";
    // %s=Entity Type, %s=Identifier Type (ID/name), %s=Identifier Value, %s=NOT_FOUND_MESSAGE
    public static final String RESOURCE_ALREADY_EXISTS_TEMPLATE = "A %s %s '%s' %s. Please choose "
            + "a unique name.";
    // %s=Entity Type, %s=Identifier Type (name), %s=Identifier Value, %s=ALREADY_EXISTS_MESSAGE

    // --- Parameter/Input Errors ---
    public static final String INVALID_QUERY_PARAMETER = "Invalid query parameter: "
            + "%s. Allowed parameters are: author, name, theme, color."; // %s=param name

    // --- Specific Cases ---
    public static final String SCHEM_FILE_FOR_BUILD_NOT_FOUND = SCHEM_FILE + " for build '%s' "
            + NOT_FOUND_MESSAGE; // %s=build identifier
}