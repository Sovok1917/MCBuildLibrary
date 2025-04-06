package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs")
@Tag(name = "Logs", description = "API for retrieving application log files (Use with caution)")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private static final String LOG_DIRECTORY = "./logs/archive";
    private static final String LOG_FILENAME_PREFIX = "mcbuildlibrary-";
    private static final String LOG_FILENAME_SUFFIX = ".log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Pattern LOG_DATE_PATTERN = Pattern.compile(
            "^" + Pattern.quote(LOG_FILENAME_PREFIX) + "(\\d{4}-\\d{2}-\\d{2})"
                    + Pattern.quote(LOG_FILENAME_SUFFIX) + "$"
    );

    @Operation(summary = "List available log dates", description = "Retrieves a list of dates "
            + "(YYYY-MM-DD) for which archived log files can be downloaded.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully "
            + "retrieved list of dates",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = String[].class))), @ApiResponse(
                                    responseCode = "500", description = "Internal error reading "
            + "log directory",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<Object> getAvailableLogDates() {
        List<String> availableDates = new ArrayList<>();
        Path archivePath = Paths.get(LOG_DIRECTORY);

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
            String errorMessage = String.format(
                    "Failed to list log files in directory '%s'. Operation failed due to: %s",
                    archivePath.toAbsolutePath(),
                    e.getClass().getSimpleName()
            );
            log.error(errorMessage, e);
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An internal error occurred while accessing the log directory.");
            pd.setTitle("Log Access Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
        }

        Collections.sort(availableDates);
        return ResponseEntity.ok(availableDates);
    }

    private Optional<String> processLogFileEntry(Path entry) {
        if (Files.isRegularFile(entry)) {
            String filename = entry.getFileName().toString();
            Matcher matcher = LOG_DATE_PATTERN.matcher(filename);
            if (matcher.matches()) {
                String dateString = matcher.group(1);
                try {
                    DATE_FORMATTER.parse(dateString);
                    return Optional.of(dateString);
                } catch (DateTimeParseException e) {
                    log.warn("Skipping file with invalid date format: {}", filename);
                }
            }
        }
        return Optional.empty();
    }

    @Operation(summary = "Download log file by date", description = "Downloads the archived "
            + "general log file for the specified date.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Log file download "
            + "initiated",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)), @ApiResponse(
                            responseCode = "400", description = "Invalid date format "
            + "(must be YYYY-MM-DD)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "404", description = "Log file not found for "
            + "the specified date",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "500", description = "Internal error reading "
            + "log file or permission issue",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{date}")
    public ResponseEntity<Object> getLogFileByDate(
            @Parameter(description = "The date of the log file to retrieve", required = true,
                    example = "2025-04-05")
            @PathVariable("date") String dateString) {

        LocalDate date = parseAndValidateDate(dateString);

        String formattedDate = date.format(DATE_FORMATTER);
        String filename = LOG_FILENAME_PREFIX + formattedDate + LOG_FILENAME_SUFFIX;
        Path logFilePath = Paths.get(LOG_DIRECTORY, filename);
        log.info("Attempting to retrieve log file: {}", logFilePath.toAbsolutePath());

        if (!Files.exists(logFilePath)) {
            log.warn("Log file path does not exist: {}", logFilePath);
            throw new NoSuchElementException("Log file not found for date: " + formattedDate);
        }

        try (InputStream inputStream = Files.newInputStream(logFilePath)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""
                    + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            log.info("Successfully prepared log file for download: {}", filename);

            long fileSize = Files.size(logFilePath);
            InputStreamResource resource = new InputStreamResource(inputStream);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);
        } catch (IOException | SecurityException e) {
            String errorMessage = String.format(
                    "Failed to access or read log file '%s'. Operation failed due to: %s",
                    logFilePath.toAbsolutePath(),
                    e.getClass().getSimpleName()
            );
            log.error(errorMessage, e);
            ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                    "An internal error occurred while accessing the log file.");
            pd.setTitle("Log Access Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
        }
    }

    private LocalDate parseAndValidateDate(String dateString) {
        try {
            if (!dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("Date string does not match YYYY-MM-DD format",
                        dateString, 0);
            }
            return LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD.", e);
        }
    }
}