package sovok.mcbuildlibrary.controller;

import java.io.IOException;
import java.nio.file.DirectoryStream; // Import for directory listing
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList; // Import ArrayList
import java.util.Collections; // Import Collections
import java.util.List;      // Import List
import java.util.NoSuchElementException;
import java.util.regex.Matcher; // Import Matcher
import java.util.regex.Pattern;  // Import Pattern
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/logs") // Removed /admin
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    // Constants for log file structure (matching logback-spring.xml)
    private static final String LOG_DIRECTORY = "./logs/archive";
    private static final String LOG_FILENAME_PREFIX = "mcbuildlibrary-"; // Prefix before date
    private static final String LOG_FILENAME_SUFFIX = ".log";      // Suffix after date
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // YYYY-MM-DD

    // Regex to extract the date from the filename
    // Matches "mcbuildlibrary-" followed by YYYY-MM-DD and ending with ".log"
    private static final Pattern LOG_DATE_PATTERN = Pattern.compile(
            "^" + Pattern.quote(LOG_FILENAME_PREFIX) + "(\\d{4}-\\d{2}-\\d{2})" + Pattern.quote(LOG_FILENAME_SUFFIX) + "$"
    );


    /**
     * Lists the dates for which archived general log files are available.
     *
     * @return ResponseEntity containing a sorted list of dates in YYYY-MM-DD format.
     */
    @GetMapping // Maps to the base path "/logs"
    public ResponseEntity<List<String>> getAvailableLogDates() {
        List<String> availableDates = new ArrayList<>();
        Path archivePath = Paths.get(LOG_DIRECTORY);

        log.info("Scanning for available log dates in: {}", archivePath.toAbsolutePath());

        if (!Files.isDirectory(archivePath)) {
            log.warn("Log archive directory not found or is not a directory: {}", archivePath);
            // Return empty list if directory doesn't exist
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Use DirectoryStream for efficient listing within a try-with-resources block
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(archivePath)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String filename = entry.getFileName().toString();
                    Matcher matcher = LOG_DATE_PATTERN.matcher(filename);
                    if (matcher.matches()) {
                        String dateString = matcher.group(1); // Extract the date part
                        // Optional: Validate if the extracted string is a valid date
                        try {
                            DATE_FORMATTER.parse(dateString); // Try parsing to validate format
                            availableDates.add(dateString);
                        } catch (DateTimeParseException e) {
                            log.warn("Found file matching pattern but with invalid date format: {}", filename);
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Error reading log archive directory: {}", archivePath, e);
            // Let GlobalExceptionHandler handle this as 500
            throw new RuntimeException("Error listing available log dates.", e);
        }

        Collections.sort(availableDates); // Sort dates chronologically
        return ResponseEntity.ok(availableDates);
    }


    /**
     * Retrieves the specific archived general log file for a given date.
     * WARNING: Exposes log files, secure appropriately in production.
     *
     * @param dateString The date in YYYY-MM-DD format.
     * @return ResponseEntity containing the log file resource or an error status.
     */
    @GetMapping("/{date}")
    public ResponseEntity<Resource> getLogFileByDate(
            // Explicitly link the "date" path variable to the dateString parameter
            @PathVariable("date") String dateString) { // <<< FIX IS HERE

        LocalDate date;
        try {
            // Ensure the input string actually conforms to the expected pattern before parsing
            if (!dateString.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new DateTimeParseException("Date string does not match YYYY-MM-DD format", dateString, 0);
            }
            date = LocalDate.parse(dateString, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Please use YYYY-MM-DD.", e);
        }

        String formattedDate = date.format(DATE_FORMATTER); // Already validated format
        String filename = LOG_FILENAME_PREFIX + formattedDate + LOG_FILENAME_SUFFIX; // Construct filename
        Path logFilePath = Paths.get(LOG_DIRECTORY, filename);

        log.info("Attempting to retrieve log file: {}", logFilePath.toAbsolutePath());

        if (!Files.exists(logFilePath)) {
            log.warn("Log file path does not exist: {}", logFilePath);
            throw new NoSuchElementException("Log file not found for date: " + formattedDate);
        }
        if (!Files.isReadable(logFilePath)) {
            log.warn("Log file exists but is not readable (check permissions): {}", logFilePath);
        }

        try {
            log.debug("Attempting to get size for: {}", logFilePath);
            long fileSize = Files.size(logFilePath);

            log.debug("Attempting to create InputStreamResource for: {}", logFilePath);
            InputStreamResource resource = new InputStreamResource(Files.newInputStream(logFilePath));

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
            headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");

            log.info("Successfully prepared log file for download: {}", filename);
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(fileSize)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(resource);

        } catch (IOException e) {
            log.error("IOException while accessing log file {}: {}", logFilePath, e.getMessage(), e);
            throw new RuntimeException("Error accessing log file: " + filename, e);
        } catch (SecurityException se) {
            log.error("SecurityException while accessing log file {} (check permissions): {}", logFilePath, se.getMessage(), se);
            throw new RuntimeException("Permission denied while accessing log file: " + filename, se);
        }
    }
}