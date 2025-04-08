package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.exception.LogAccessException;
import sovok.mcbuildlibrary.exception.StringConstants;

@RestController
// Use constants from StringConstants
@RequestMapping(StringConstants.BASE_LOGS_PATH)
@Tag(name = StringConstants.LOGS_TAG_NAME, description = StringConstants.LOGS_TAG_DESCRIPTION)
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    // --- getAvailableLogDates, processLogFileEntry, getLogFileByDate,
    // getTodaysLogFile, parseAndValidateDate ---
    // --- remain unchanged from the previous correct version ---

    @Operation(summary = StringConstants.LIST_DATES_SUMMARY, description
            = StringConstants.LIST_DATES_DESCRIPTION)
    @ApiResponses()
    @GetMapping
    public ResponseEntity<Object> getAvailableLogDates() {
        List<String> availableDates = new ArrayList<>();
        Path archivePath = Paths.get(StringConstants.ARCHIVE_LOG_DIRECTORY);

        log.info("Scanning for available log dates in: {}", archivePath.toAbsolutePath());

        if (!Files.isDirectory(archivePath)) {
            log.warn("Log archive directory not found or is not a directory: {}", archivePath);
            return ResponseEntity.ok(Collections.emptyList());
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archivePath)) {
            for (Path entry : stream) {
                processLogFileEntry(entry).ifPresent(availableDates::add);
            }
        } catch (IOException | SecurityException e) {
            String contextualMessage = String.format(
                    "%s Accessing directory: %s. Cause: %s - %s",
                    StringConstants.LOG_DIRECTORY_ACCESS_ERROR_DETAIL,
                    archivePath.toAbsolutePath(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new LogAccessException(contextualMessage, e);
        }

        Collections.sort(availableDates);
        return ResponseEntity.ok(availableDates);
    }

    private Optional<String> processLogFileEntry(Path entry) {
        if (Files.isRegularFile(entry)) {
            String filename = entry.getFileName().toString();
            Matcher matcher = StringConstants.ARCHIVED_LOG_DATE_PATTERN.matcher(filename);
            if (matcher.matches()) {
                String dateString = matcher.group(1);
                try {
                    StringConstants.LOG_DATE_FORMATTER.parse(dateString);
                    return Optional.of(dateString);
                } catch (DateTimeParseException e) {
                    log.warn("Skipping file with invalid date format in name: {}. Reason: {}",
                            filename, e.getMessage());
                }
            } else {
                log.trace("Skipping file not matching archive pattern: {}", filename);
            }
        }
        return Optional.empty();
    }

    @Operation(summary = StringConstants.DOWNLOAD_BY_DATE_SUMMARY, description
            = StringConstants.DOWNLOAD_BY_DATE_DESCRIPTION)
    @ApiResponses()
    @GetMapping("/{" + StringConstants.LOG_DATE_PATH_VARIABLE + ":"
            + StringConstants.LOG_DATE_REGEX + "}")
    public ResponseEntity<Object> getLogFileByDate(
            @Parameter(description = StringConstants.LOG_DATE_PARAM_DESCRIPTION, required = true,
                    example = StringConstants.LOG_DATE_EXAMPLE)
            @PathVariable(StringConstants.LOG_DATE_PATH_VARIABLE) String dateString) {

        LocalDate date = parseAndValidateDate(dateString);

        String formattedDate = date.format(StringConstants.LOG_DATE_FORMATTER);
        String filename = StringConstants.ARCHIVED_LOG_FILENAME_PREFIX + formattedDate
                + StringConstants.LOG_FILENAME_SUFFIX;
        Path logFilePath = Paths.get(StringConstants.ARCHIVE_LOG_DIRECTORY, filename);

        log.info("Attempting to retrieve archived log file: {}", logFilePath.toAbsolutePath());

        if (!Files.exists(logFilePath) || !Files.isRegularFile(logFilePath)) {
            throw new NoSuchElementException(String.format(
                    StringConstants.LOG_FILE_NOT_FOUND_FOR_DATE, formattedDate));
        }
        return serveLogFile(logFilePath, filename);
    }

    @Operation(summary = StringConstants.DOWNLOAD_TODAY_SUMMARY, description =
            StringConstants.DOWNLOAD_TODAY_DESCRIPTION)
    @ApiResponses()
    @GetMapping(StringConstants.TODAY_LOG_ENDPOINT_PATH)
    public ResponseEntity<Object> getTodaysLogFile() {
        Path logFilePath = Paths.get(StringConstants.ACTIVE_LOG_FILE_PATH);
        log.info("Attempting to retrieve today's active log file: {}",
                logFilePath.toAbsolutePath());

        if (!Files.exists(logFilePath) || !Files.isRegularFile(logFilePath)) {
            throw new NoSuchElementException(StringConstants.TODAY_LOG_FILE_NOT_FOUND);
        }
        return serveLogFile(logFilePath, StringConstants.ACTIVE_LOG_FILENAME);
    }

    private LocalDate parseAndValidateDate(String dateString) {
        try {
            return LocalDate.parse(dateString, StringConstants.LOG_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            String contextualMessage = String.format("%s Input was: '%s'",
                    StringConstants.INVALID_DATE_FORMAT_DETAIL,
                    dateString);
            throw new IllegalArgumentException(contextualMessage, e);
        }
    }

    // *** MODIFIED METHOD ***
    private ResponseEntity<Object> serveLogFile(Path logFilePath, String downloadFilename) {
        InputStream inputStream; // Declare outside try
        try {
            // *** REMOVED try-with-resources ***
            // Create the input stream - MUST NOT be closed here
            inputStream = Files.newInputStream(logFilePath);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, String.format(
                    StringConstants.HEADER_CONTENT_DISPOSITION_FORMAT, downloadFilename));
            headers.add(HttpHeaders.CACHE_CONTROL, StringConstants.HEADER_CACHE_CONTROL_NO_CACHE);
            headers.add(HttpHeaders.PRAGMA, StringConstants.HEADER_PRAGMA_NO_CACHE);
            headers.add(HttpHeaders.EXPIRES, StringConstants.HEADER_EXPIRES_ZERO);

            // Get size *before* creating resource, can throw IOException
            long fileSize = Files.size(logFilePath);

            // Pass the OPEN stream to the resource
            InputStreamResource resource = new InputStreamResource(inputStream);

            log.info("Successfully prepared log file for download: {}", downloadFilename);

            // Return the response entity; Spring will handle reading the stream
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);

        } catch (IOException | SecurityException e) {
            // If creating stream or getting size fails, we might need to close the stream
            // IF it was successfully opened before the error occurred.
            // However, Files.newInputStream throws before assignment on error,
            // and Files.size also throws before resource creation.
            // The stream passed to InputStreamResource will be managed by Spring.
            // So, no explicit close needed here in the catch block.

            // Rethrow for GlobalExceptionHandler (without local logging)
            String contextualMessage = String.format(
                    "%s Accessing file: %s. Cause: %s - %s",
                    StringConstants.LOG_FILE_ACCESS_ERROR_DETAIL,
                    logFilePath.toAbsolutePath(),
                    e.getClass().getSimpleName(),
                    e.getMessage()
            );
            throw new LogAccessException(contextualMessage, e);
        }
        // Note: No finally block needed to close 'inputStream' as Spring will manage the
        // stream wrapped in InputStreamResource when processing the response body.
    }
}