// file: src/main/java/sovok/mcbuildlibrary/controller/BuildLogController.java
package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sovok.mcbuildlibrary.dto.TaskState;
import sovok.mcbuildlibrary.dto.TaskStatusDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.service.BuildLogService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping(StringConstants.BUILDS_ENDPOINT) // Base path from BuildController
@Tag(name = StringConstants.BUILD_LOGS_TAG_NAME, description = StringConstants.BUILD_LOGS_TAG_DESCRIPTION)
public class BuildLogController {

    private static final Logger log = LoggerFactory.getLogger(BuildLogController.class);

    private final BuildLogService buildLogService;

    public BuildLogController(BuildLogService buildLogService) {
        this.buildLogService = buildLogService;
    }

    @Operation(summary = StringConstants.GENERATE_LOG_SUMMARY, description = StringConstants.GENERATE_LOG_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = StringConstants.LOG_GENERATION_INITIATED,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class, example = "{\"taskId\": \"" + StringConstants.TASK_ID_EXAMPLE + "\"}"))),
            @ApiResponse(responseCode = "404", description = StringConstants.NOT_FOUND_MESSAGE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{identifier}/generate-log")
    public ResponseEntity<Map<String, String>> generateBuildLog(
            @Parameter(description = "ID or exact name of the build", required = true, example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        try {
            String taskId = buildLogService.initiateLogGeneration(identifier);
            log.info("Log generation request accepted for identifier '{}', task ID: {}", identifier, taskId);
            return ResponseEntity.accepted().body(Map.of("taskId", taskId));
        } catch (NoSuchElementException e) {
            // Let GlobalExceptionHandler handle this for a 404 ProblemDetail
            throw e;
        }
    }

    @Operation(summary = StringConstants.GET_LOG_STATUS_SUMMARY, description = StringConstants.GET_LOG_STATUS_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task status retrieved successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TaskStatusDto.class))),
            @ApiResponse(responseCode = "400", description = StringConstants.INVALID_TASK_ID_FORMAT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = StringConstants.NOT_FOUND_MESSAGE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/log-status/{taskId}")
    public ResponseEntity<TaskStatusDto> getLogGenerationStatus(
            @Parameter(description = StringConstants.TASK_ID_PARAM_DESCRIPTION, required = true, example = StringConstants.TASK_ID_EXAMPLE)
            @PathVariable(StringConstants.TASK_ID_PATH_VAR) String taskId) {

        if (!isValidUuid(taskId)) {
             throw new IllegalArgumentException(StringConstants.INVALID_TASK_ID_FORMAT);
        }

        Optional<TaskStatusDto> statusOpt = buildLogService.getTaskStatus(taskId);

        return statusOpt
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.LOG_TASK_NOT_FOUND, taskId)));
    }

    @Operation(summary = StringConstants.GET_LOG_FILE_SUMMARY, description = StringConstants.GET_LOG_FILE_DESCRIPTION)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Log file retrieved successfully",
                    content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
            @ApiResponse(responseCode = "202", description = StringConstants.LOG_GENERATION_IN_PROGRESS,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Map.class, example = "{\"message\": \"" + StringConstants.LOG_GENERATION_IN_PROGRESS + "\"}"))),
            @ApiResponse(responseCode = "400", description = StringConstants.INVALID_TASK_ID_FORMAT,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = StringConstants.NOT_FOUND_MESSAGE + " or " + StringConstants.LOG_GENERATION_FAILED,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/log-file/{taskId}")
    public ResponseEntity<?> getLogFile(
            @Parameter(description = StringConstants.TASK_ID_PARAM_DESCRIPTION, required = true, example = StringConstants.TASK_ID_EXAMPLE)
            @PathVariable(StringConstants.TASK_ID_PATH_VAR) String taskId) {

         if (!isValidUuid(taskId)) {
             throw new IllegalArgumentException(StringConstants.INVALID_TASK_ID_FORMAT);
        }

        Optional<TaskStatusDto> statusOpt = buildLogService.getTaskStatus(taskId);

        if (statusOpt.isEmpty()) {
            throw new NoSuchElementException(String.format(StringConstants.LOG_TASK_NOT_FOUND, taskId));
        }

        TaskStatusDto status = statusOpt.get();

        switch (status.status()) {
            case COMPLETED:
                Path filePath = Paths.get(status.filePath());
                if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                    log.error("Log file path found in cache for task {}, but file does not exist or is not a regular file: {}", taskId, filePath);
                    // Treat as not found, as the file is missing
                     throw new NoSuchElementException("Log file for task " + taskId + " not found on server.");
                }
                Resource resource = new FileSystemResource(filePath);
                String filename = filePath.getFileName().toString();
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_PLAIN)
                        .header(HttpHeaders.CONTENT_DISPOSITION, String.format(StringConstants.HEADER_CONTENT_DISPOSITION_FORMAT, filename))
                        .body(resource);

            case PENDING:
                return ResponseEntity.accepted().body(Map.of("message", StringConstants.LOG_GENERATION_IN_PROGRESS));

            case FAILED:
                 // Return 404 for failed tasks as the resource (log file) is not available
                 throw new NoSuchElementException(StringConstants.LOG_GENERATION_FAILED + (status.errorMessage() != null ? ": " + status.errorMessage() : ""));

            default:
                // Should not happen
                log.error("Unexpected task status {} for task ID {}", status.status(), taskId);
                ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected task status.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
        }
    }

    // --- Private Helper ---
    private boolean isValidUuid(String uuid) {
        if (uuid == null) return false;
        try {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}