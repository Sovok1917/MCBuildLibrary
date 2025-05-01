// file: src/main/java/sovok/mcbuildlibrary/service/BuildLogService.java
package sovok.mcbuildlibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.TaskState;
import sovok.mcbuildlibrary.dto.TaskStatusDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class BuildLogService {

    private static final Logger log = LoggerFactory.getLogger(BuildLogService.class);
    private static final long ARTIFICIAL_DELAY_MS = 3000; // Keep delay

    private final BuildService buildService;
    private final InMemoryCache cache;
    private final Executor taskExecutor;

    public BuildLogService(BuildService buildService, InMemoryCache cache,
                           @Qualifier("taskExecutor") Executor taskExecutor) {
        this.buildService = buildService;
        this.cache = cache;
        this.taskExecutor = taskExecutor;
        createLogDirectory();
    }

    private void createLogDirectory() {
        Path logDirPath = Paths.get(StringConstants.BUILD_LOGS_DIRECTORY);
        if (!Files.exists(logDirPath)) {
            try {
                Files.createDirectories(logDirPath);
                log.info("Created build log directory: {}", logDirPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to create build log directory: {}", logDirPath.toAbsolutePath(), e);
            }
        }
    }

    public String initiateLogGeneration(String identifier) {
        Build buildStub = findBuildByIdentifier(identifier);
        Long buildId = buildStub.getId();
        String taskId = UUID.randomUUID().toString();
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);

        TaskStatusDto initialStatus = TaskStatusDto.pending(taskId);
        cache.put(cacheKey, initialStatus);
        log.info("Initiating log generation for build ID {} with task ID: {}", buildId, taskId);

        CompletableFuture.runAsync(() -> performLogGeneration(buildId, taskId), taskExecutor)
                .exceptionally(ex -> {
                    log.error("Unhandled exception wrapper caught for async task ID {}: {}", taskId, ex.getMessage(), ex);
                    Optional<TaskStatusDto> currentStatus = getTaskStatus(taskId);
                    if (currentStatus.isEmpty() || currentStatus.get().status() != TaskState.FAILED) {
                        updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Unexpected error during async execution wrapper."));
                    }
                    return null;
                });

        return taskId;
    }

    @Async("taskExecutor")
    @Transactional(readOnly = true)
    public void performLogGeneration(Long buildId, String taskId) {
        log.info("Starting async log generation for build ID {}, task ID {}", buildId, taskId);

        try {
            log.info("Task ID {}: Introducing artificial delay of {} ms...", taskId, ARTIFICIAL_DELAY_MS);
            Thread.sleep(ARTIFICIAL_DELAY_MS);
            log.info("Task ID {}: Artificial delay finished.", taskId);
        } catch (InterruptedException e) {
            log.warn("Task ID {}: Log generation task interrupted during artificial delay.", taskId);
            Thread.currentThread().interrupt();
            updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Task interrupted during processing."));
            return;
        }

        try {
            Optional<Build> buildOpt = buildService.findBuildFullyLoadedForLog(buildId);

            if (buildOpt.isEmpty()) {
                log.error("Build ID {} not found (with full load attempt) for task ID {}.", buildId, taskId);
                updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Build not found during generation."));
                return;
            }
            Build build = buildOpt.get();

            log.info("Task ID {}: Building log content...", taskId);
            String logContent = buildLogContent(build);

            Path logFilePath = Paths.get(StringConstants.BUILD_LOGS_DIRECTORY,
                    String.format(StringConstants.BUILD_LOG_FILENAME_TEMPLATE, buildId));

            log.info("Task ID {}: Writing log file to {}...", taskId, logFilePath.toAbsolutePath());
            try (BufferedWriter writer = Files.newBufferedWriter(logFilePath)) {
                writer.write(logContent);
                log.info("Successfully generated log file for task ID {} at: {}", taskId, logFilePath.toAbsolutePath());
            } catch (IOException e) {
                log.error("IOException while writing log file for task ID {}: {}", taskId, e.getMessage(), e);
                updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Failed to write log file: " + e.getMessage()));
                return;
            }

            updateTaskStatus(taskId, TaskStatusDto.completed(taskId, logFilePath.toString()));

        } catch (Exception e) {
            if (e instanceof org.hibernate.LazyInitializationException) {
                log.error("LazyInitializationException likely accessing schemFile for task ID {}: {}", taskId, e.getMessage());
                updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Internal error accessing build data (LOB)."));
            } else {
                log.error("Unexpected error during log generation for task ID {}: {}", taskId, e.getMessage(), e);
                updateTaskStatus(taskId, TaskStatusDto.failed(taskId, "Internal error during log generation: " + e.getClass().getSimpleName()));
            }
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
        }
    }

    public Optional<TaskStatusDto> getTaskStatus(String taskId) {
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);
        return cache.get(cacheKey);
    }

    private String buildLogContent(Build build) {
        StringBuilder sb = new StringBuilder();
        sb.append("Minecraft Build Log\n");
        sb.append("=====================\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        sb.append("Build ID: ").append(build.getId()).append("\n");
        sb.append("Build Name: ").append(build.getName()).append("\n\n");

        // Authors, Themes, Colors, Description, Screenshots sections remain the same...
        sb.append("Authors:\n");
        if (build.getAuthors() != null && !build.getAuthors().isEmpty()) {
            build.getAuthors().stream()
                    .sorted(Comparator.comparing(Author::getName))
                    .forEach(author ->
                            sb.append("  - ID: ").append(author.getId()).append(", Name: ").append(author.getName()).append("\n")
                    );
        } else {
            sb.append("  (None)\n");
        }
        sb.append("\n");

        sb.append("Themes:\n");
        if (build.getThemes() != null && !build.getThemes().isEmpty()) {
            build.getThemes().stream()
                    .sorted(Comparator.comparing(Theme::getName))
                    .forEach(theme ->
                            sb.append("  - ID: ").append(theme.getId()).append(", Name: ").append(theme.getName()).append("\n")
                    );
        } else {
            sb.append("  (None)\n");
        }
        sb.append("\n");

        sb.append("Colors:\n");
        if (build.getColors() != null && !build.getColors().isEmpty()) {
            build.getColors().stream()
                    .sorted(Comparator.comparing(Color::getName))
                    .forEach(color ->
                            sb.append("  - ID: ").append(color.getId()).append(", Name: ").append(color.getName()).append("\n")
                    );
        } else {
            sb.append("  (None)\n");
        }
        sb.append("\n");

        sb.append("Description:\n");
        sb.append(build.getDescription() != null && !build.getDescription().isBlank() ? build.getDescription() : "  (None)").append("\n\n");

        sb.append("Screenshots:\n");
        if (build.getScreenshots() != null && !build.getScreenshots().isEmpty()) {
            build.getScreenshots().stream().sorted().forEach(screenshot ->
                    sb.append("  - ").append(screenshot).append("\n")
            );
        } else {
            sb.append("  (None)\n");
        }
        sb.append("\n");

        // --- MODIFIED SCHEMATIC FILE SECTION ---
        sb.append("Schematic File:\n");
        byte[] schemBytes = null;
        try {
            schemBytes = build.getSchemFile(); // Access schemFile (triggers lazy load if needed)
            if (schemBytes != null) {
                // File data exists in the record (byte array is not null)
                sb.append("  Size: ").append(schemBytes.length).append(" bytes\n"); // Show size, even if 0
            } else {
                // The schemFile field itself is null in the database
                sb.append("  (Not present)\n");
            }
        } catch (org.hibernate.LazyInitializationException lie) {
            log.warn("Could not lazy-load schemFile within buildLogContent: {}", lie.getMessage());
            sb.append("  (Error loading file data)\n");
        } catch (Exception e) {
            // Catch other potential errors during access
            log.error("Error accessing schemFile data: {}", e.getMessage(), e);
            sb.append("  (Error accessing file data)\n");
        }
        // --- END MODIFIED SECTION ---
        sb.append("\n");


        sb.append("=====================\n");
        sb.append("End of Log\n");

        return sb.toString();
    }

    private void updateTaskStatus(String taskId, TaskStatusDto statusDto) {
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);
        cache.put(cacheKey, statusDto);
        log.info("Updated status for task ID {} to: {}", taskId, statusDto.status());
    }

    private Build findBuildByIdentifier(String identifier) {
        return buildService.findBuildByIdentifier(identifier);
    }
}