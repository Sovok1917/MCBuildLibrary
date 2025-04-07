package sovok.mcbuildlibrary.exception;


public final class StringConstants {

    private StringConstants() {
        // Private constructor to prevent instantiation
    }

    // --- General ---
    public static final String NOT_FOUND_MESSAGE = "not found";
    public static final String ALREADY_EXISTS_MESSAGE = "already exists";
    public static final String CANNOT_DELETE_ASSOCIATED = "Cannot delete %s '%s' because it is as"
            + "sociated with %d build(s)."; // %s=entity type, %s=name, %d=count
    public static final String QUERY_NO_RESULTS = "No %s found matching the query: %s"; // %s=ent
    // ity type plural, %s=query details
    // ity type plural
    public static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    public static final String INPUT_ERROR_MESSAGE = "Input Error";
    public static final String TYPE_MISMATCH_MESSAGE = "Invalid value '%s' for parameter '%s'. E"
            + "xpected type '%s'."; // %s=value, %s=param name, %s=expected type
    public static final String MISSING_PARAMETER_MESSAGE = "Required parameter '%s' of type '%s' "
            + "is missing."; // %s=param name, %s=param type
    public static final String MISSING_FILE_PART_MESSAGE = "Required file part '%s' is missing.";
    // <<< ADD THIS LINE (%s=part name)


    // --- Resource Identifiers ---
    public static final String WITH_ID = "with ID";
    public static final String WITH_NAME = "with name";

    // --- Entity Specific ---
    public static final String AUTHOR = "Author";
    public static final String AUTHORS = "Authors";
    public static final String BUILD = "Build";
    public static final String COLOR = "Color";
    public static final String THEME = "Theme";
    public static final String SCHEM_FILE = "Schem file";


    // --- Resource Specific Messages ---
    public static final String RESOURCE_NOT_FOUND_TEMPLATE = "%s %s '%s' %s"; // %s=Entity Type,
    // %s=Identifier Type (ID/name), %s=Identifier Value, %s=NOT_FOUND_MESSAGE
    public static final String RESOURCE_ALREADY_EXISTS_TEMPLATE = "A %s %s '%s' %s. Please choos"
            + "e a unique name."; // %s=Entity Type, %s=Identifier Type (name), %s=Identifier
    // Value, %s=ALREADY_EXISTS_MESSAGE

    // --- Parameter/Input Errors ---
    public static final String SCHEM_FILE_FOR_BUILD_NOT_FOUND = SCHEM_FILE + " for build '%s' "

            + NOT_FOUND_MESSAGE; // %s=build identifier

    // --- Request/Path Parameter Names (used in controllers) ---
    public static final String IDENTIFIER_PATH_VAR = "identifier";
    public static final String NAME_REQ_PARAM = "name";
    public static final String AUTHORS_REQ_PARAM = "authors";
    public static final String THEMES_REQ_PARAM = "themes";
    public static final String DESCRIPTION_REQ_PARAM = "description";
    public static final String COLORS_REQ_PARAM = "colors";
    public static final String SCREENSHOTS_REQ_PARAM = "screenshots";
    public static final String SCHEM_FILE_REQ_PARAM = "schemFile";
    public static final String AUTHOR_QUERY_PARAM = "author";
    public static final String THEME_QUERY_PARAM = "theme";
    public static final String COLOR_QUERY_PARAM = "color";

    // --- Validation Messages ---
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String NAME_SIZE = "Name must be between {min} and {max} characters";
    public static final String FILE_NOT_EMPTY = "File cannot be empty";
    public static final String LOG_MESSAGE_FORMAT = "{}: {}";
    public static final String NAME_NOT_ONLY_NUMERIC = "Name must not consist of only numeric "
            + "characters";

}