// file: src/main/java/sovok/mcbuildlibrary/exception/StringConstants.java
package sovok.mcbuildlibrary.exception;

import java.util.Set;

public final class StringConstants {

    private StringConstants() {
        // Private constructor to prevent instantiation
    }

    // --- General ---
    public static final String INVALID_QUERY_PARAMETER_DETECTED = "Invalid query parameter '%s' detected. Allowed parameters for this endpoint are: %s.";
    public static final String NOT_FOUND_MESSAGE = "not found";
    public static final String ALREADY_EXISTS_MESSAGE = "already exists";
    public static final String CANNOT_DELETE_ASSOCIATED = "Cannot delete %s '%s' because it is associated with %d build(s)."; // %s=entity type, %s=name, %d=count
    public static final String QUERY_NO_RESULTS = "No %s found matching the query: %s"; // %s=entity type plural, %s=query details
    public static final String NO_ENTITIES_AVAILABLE = "No %s are currently available"; // %s=entity type plural
    public static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    public static final String INPUT_ERROR_MESSAGE = "Input Error";
    public static final String TYPE_MISMATCH_MESSAGE = "Invalid value '%s' for parameter '%s'. Expected type '%s'."; // %s=value, %s=param name, %s=expected type
    public static final String MISSING_PARAMETER_MESSAGE = "Required parameter '%s' of type '%s' is missing."; // %s=param name, %s=param type
    public static final String MISSING_FILE_PART_MESSAGE = "Required file part '%s' is missing."; // <<< ADD THIS LINE (%s=part name)


    // --- Resource Identifiers ---
    public static final String WITH_ID = "with ID";
    public static final String WITH_NAME = "with name";

    // --- Entity Specific ---
    public static final String AUTHOR = "Author";
    public static final String AUTHORS = "authors";
    public static final String BUILD = "Build";
    public static final String BUILDS = "builds";
    public static final String COLOR = "Color";
    public static final String COLORS = "colors";
    public static final String THEME = "Theme";
    public static final String THEMES = "themes";
    public static final String SCHEM_FILE = "Schem file";
    public static final String SCREENSHOT = "Screenshot";


    // --- Resource Specific Messages ---
    public static final String RESOURCE_NOT_FOUND_TEMPLATE = "%s %s '%s' %s"; // %s=Entity Type, %s=Identifier Type (ID/name), %s=Identifier Value, %s=NOT_FOUND_MESSAGE
    public static final String RESOURCE_ALREADY_EXISTS_TEMPLATE = "A %s %s '%s' %s. Please choose a unique name."; // %s=Entity Type, %s=Identifier Type (name), %s=Identifier Value, %s=ALREADY_EXISTS_MESSAGE
    public static final String SCREENSHOT_INDEX_NOT_FOUND = "%s index %d for %s %s '%s' %s"; // Screenshot, index, Build, with ID, buildId, not found

    // --- Parameter/Input Errors ---
    public static final String INVALID_QUERY_PARAMETER = "Invalid query parameter: %s. Allowed parameters are: author, name, theme, color."; // %s=param name
    public static final String SCHEM_FILE_FOR_BUILD_NOT_FOUND = SCHEM_FILE + " for build '%s' " + NOT_FOUND_MESSAGE; // %s=build identifier

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
    public static final String INDEX_QUERY_PARAM = "index";

    public static final Set<String> ALLOWED_BUILD_QUERY_PARAMS = Set.of(
            AUTHOR_QUERY_PARAM, // "author"
            NAME_REQ_PARAM,     // "name"
            THEME_QUERY_PARAM,  // "theme"
            COLOR_QUERY_PARAM   // "color"
    );

    public static final Set<String> ALLOWED_SIMPLE_QUERY_PARAMS = Set.of(
            NAME_REQ_PARAM      // "name" - Used for Author, Color, Theme queries
    );


    // --- Validation Messages ---
    public static final String NAME_NOT_BLANK = "Name cannot be blank";
    public static final String NAME_SIZE = "Name must be between {min} and {max} characters";
    public static final String LIST_NOT_EMPTY = "List cannot be empty";
    public static final String FILE_NOT_EMPTY = "File cannot be empty";
    public static final String INDEX_NON_NEGATIVE = "Index must be zero or positive";

}