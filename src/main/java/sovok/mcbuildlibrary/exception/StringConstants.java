package sovok.mcbuildlibrary.exception;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class StringConstants {



    private StringConstants() {
        // Private constructor to prevent instantiation
    }

    // --- General ---
    public static final String NOT_FOUND_MESSAGE = "not found";
    public static final String ALREADY_EXISTS_MESSAGE = "already exists";
    public static final String CANNOT_DELETE_ASSOCIATED = "Cannot delete %s '%s' because it"
            + " is associated with %d build(s)."; // %s=entity type, %s=name, %d=count
    public static final String QUERY_NO_RESULTS = "No %s found matching the query: %s"; //
    // %s=entity type plural, %s=query details
    public static final String VALIDATION_FAILED_MESSAGE = "Validation failed";
    public static final String INPUT_ERROR_MESSAGE = "Input Error";
    public static final String TYPE_MISMATCH_MESSAGE = "Invalid value '%s' for parameter '%s"
            + "'. Expected type '%s'."; // %s=value, %s=param name, %s=expected type
    public static final String MISSING_PARAMETER_MESSAGE = "Required parameter '%s' of type "
            + "'%s' is missing."; // %s=param name, %s=param type
    public static final String MISSING_FILE_PART_MESSAGE = "Required file part '%s' is miss"
            + "ing."; // %s=part name
    public static final String BULK_OPERATION_SUCCESS = "Bulk operation completed."; // New

    // --- Resource Identifiers ---
    public static final String WITH_ID = "with ID";
    public static final String WITH_NAME = "with name";

    // --- Entity Specific ---
    public static final String AUTHOR = "Author";
    public static final String AUTHORS = "Authors";
    public static final String BUILD = "Build";
    public static final String BUILDS = "Builds";
    public static final String COLORS = "Colors";
    public static final String COLOR = "Color";
    public static final String THEME = "Theme";
    public static final String THEMES = "Themes";
    public static final String SCHEM_FILE = "Schem file";

    // --- Resource Specific Messages ---
    public static final String RESOURCE_NOT_FOUND_TEMPLATE = "%s %s '%s' %s"; // %s=Entity Type, %s/
    // / =Identifier Type (ID/name), %s=Identifier Value, %s=NOT_FOUND_MESSAGE
    public static final String RESOURCE_ALREADY_EXISTS_TEMPLATE = "A %s %s '%s' %s. Please choose "
            + "a unique name."; // %s=Entity Type, %s=Identifier Type (name), %s=Identifier Valu
    // e, %s=ALREADY_EXISTS_MESSAGE

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
    public static final String NAME_NOT_ONLY_NUMERIC = "Name must not consist of only numeric char"
            + "acters";


    // --- Logging Specific ---
    // --- Directory and File Naming ---
    public static final String FORWARD_SLASH = "/";
    public static final String LOGS_BASE_DIRECTORY = "./logs"; // Base directory
    public static final String ARCHIVE_LOG_SUBDIRECTORY = "archive"; // Subdirectory for archives
    public static final String ARCHIVE_LOG_DIRECTORY = LOGS_BASE_DIRECTORY + "/"
            + ARCHIVE_LOG_SUBDIRECTORY; // Full path to archives
    public static final String APP_LOG_NAME_BASE = "mcbuildlibrary"; // Base name from logback co
    // nfig
    public static final String LOG_FILENAME_SUFFIX = ".log";
    public static final String ACTIVE_LOG_FILENAME = APP_LOG_NAME_BASE + LOG_FILENAME_SUFFIX; // e
    // .g., mcbuildlibrary.log
    public static final String ACTIVE_LOG_FILE_PATH = LOGS_BASE_DIRECTORY + FORWARD_SLASH
            + ACTIVE_LOG_FILENAME; // e.g., ./logs/mcbuildlibrary.log
    public static final String ARCHIVED_LOG_FILENAME_PREFIX = APP_LOG_NAME_BASE + "-"; // e
    // .g., mcbuildlibrary-

    // --- Path Variables and Endpoints (Logs) ---
    public static final String BASE_LOGS_PATH = "/logs";
    public static final String LOG_DATE_PATH_VARIABLE = "date"; // Renamed for clarity within
    // this section
    public static final String TODAY_LOG_ENDPOINT_PATH = "/today"; // Endpoint path for today's
    // log

    // --- Path Variables and Endpoints (General API) ---  // New Section
    public static final String API_BASE_PATH = ""; // Example base path
    public static final String AUTHORS_ENDPOINT = API_BASE_PATH + "/authors";
    public static final String BUILDS_ENDPOINT = API_BASE_PATH + "/builds";
    public static final String COLORS_ENDPOINT = API_BASE_PATH + "/colors";
    public static final String THEMES_ENDPOINT = API_BASE_PATH + "/themes";
    public static final String BULK_OPERATIONS_ENDPOINT = API_BASE_PATH + "/bulk"; // New
    public static final String BULK_CREATE_METADATA_ENDPOINT = "/create-metadata"; // New

    // --- Date Formatting and Parsing (Logs) ---
    public static final DateTimeFormatter LOG_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    // YYYY-MM-DD
    public static final String LOG_DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}"; // Regex for date validation
    public static final Pattern ARCHIVED_LOG_DATE_PATTERN = Pattern.compile(
            "^" + Pattern.quote(ARCHIVED_LOG_FILENAME_PREFIX) + "(" + LOG_DATE_REGEX + ")"
                    + Pattern.quote(LOG_FILENAME_SUFFIX) + "$"
    );

    // --- API Descriptions and Examples (Logs) ---
    public static final String LOG_DATE_PARAM_DESCRIPTION = "The date of the archived log file "
            + "to retrieve (YYYY-MM-DD)";
    public static final String LOG_DATE_EXAMPLE = "2025-04-08"; // Use current date as example
    public static final String LOGS_TAG_NAME = "Logs";
    public static final String LOGS_TAG_DESCRIPTION = "API for retrieving application log files"
            + " (Use with caution)";
    public static final String LIST_DATES_SUMMARY = "List available archived log dates";
    public static final String LIST_DATES_DESCRIPTION = "Retrieves a list of dates (YYYY-MM-DD)"
            + " for which archived log files can be downloaded.";
    public static final String DOWNLOAD_BY_DATE_SUMMARY = "Download archived log file by date";
    public static final String DOWNLOAD_BY_DATE_DESCRIPTION = "Downloads the archived general lo"
            + "g file for the specified date (YYYY-MM-DD).";
    public static final String DOWNLOAD_TODAY_SUMMARY = "Download today's active log file";
    public static final String DOWNLOAD_TODAY_DESCRIPTION = "Downloads the currently active log "
            + "file for today.";

    // --- API Descriptions (Bulk Operations) --- // New Section
    public static final String BULK_TAG_NAME = "Bulk Operations";
    public static final String BULK_TAG_DESCRIPTION = "API for performing bulk actions";
    public static final String BULK_CREATE_SUMMARY = "Bulk create Authors, Themes, and Colors";
    public static final String BULK_CREATE_DESCRIPTION = "Creates multiple Authors, "
            + "Themes, and Colors "
            + "from the provided lists of names. Existing names will be skipped.";

    // --- Response Messages and Titles (Logs) ---
    public static final String LOG_DIRECTORY_ACCESS_ERROR_DETAIL = "An internal error occurred wh"
            + "ile accessing the log directory.";
    public static final String LOG_FILE_ACCESS_ERROR_DETAIL = "An internal error occurred while "
            + "accessing the log file.";
    public static final String LOG_FILE_NOT_FOUND_FOR_DATE = "Log file not found for date: %s";
    public static final String TODAY_LOG_FILE_NOT_FOUND = "Today's active log file not found or n"
            + "ot yet created.";
    public static final String INVALID_DATE_FORMAT_DETAIL = "Invalid date format. Please use YYY"
            + "Y-MM-DD.";

    // --- HTTP Header Values ---
    public static final String HEADER_CONTENT_DISPOSITION_FORMAT = "attachment; file"
            + "name=\"%s\""; // %s = filename
    public static final String HEADER_CACHE_CONTROL_NO_CACHE = "no-cache, no-store, mus"
            + "t-revalidate";
    public static final String HEADER_PRAGMA_NO_CACHE = "no-cache";
    public static final String HEADER_EXPIRES_ZERO = "0";
}