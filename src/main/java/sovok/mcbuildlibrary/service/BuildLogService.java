package sovok.mcbuildlibrary.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import org.hibernate.LazyInitializationException; // Specific import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy; // Import Lazy
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.TaskState;
import sovok.mcbuildlibrary.dto.TaskStatusDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.BaseNamedEntity;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;

/**
 * Service responsible for generating detailed log files for Minecraft builds asynchronously.
 * It handles task initiation, status tracking via cache, and log file creation.
 */
@Service
public class BuildLogService {

    private static final Logger log = LoggerFactory.getLogger(BuildLogService.class);
    private static final long ARTIFICIAL_DELAY_MS = 3000;

    // Constants for log formatting
    private static final String LOG_PREFIX_ID = "  - ID: ";
    private static final String LOG_PREFIX_NAME = ", Name: ";
    private static final String LOG_NONE = "  (None)\n";
    private static final String LOG_SEPARATOR = "=====================\n";
    private static final String LOG_LINE_BREAK = "\n";
    private static final String LOG_INDENT = "  ";

    private final BuildService buildService;
    private final InMemoryCache cache;
    private final Executor taskExecutor;

    // Self-injection to handle transactional proxy calls for async methods
    private BuildLogService self;

    /**
     * Constructs the BuildLogService.
     *
     * @param buildService The service for accessing build data.
     * @param cache        The cache for storing task status.
     * @param taskExecutor The executor for running async tasks.
     */
    // Constructor injection remains preferred for mandatory dependencies
    public BuildLogService(BuildService buildService, InMemoryCache cache,
                           @Qualifier("taskExecutor") Executor taskExecutor) {
        this.buildService = buildService;
        this.cache = cache;
        this.taskExecutor = taskExecutor;
        createLogDirectory();
    }

    /**
     * Setter for self-injection to allow calling proxied methods.
     * Marked as {@code @Lazy} to break potential circular dependency cycles during startup.
     *
     * @param self The proxied instance of this service.
     */
    @Autowired
    @Lazy
    public void setSelf(BuildLogService self) {
        this.self = self;
    }


    /**
     * Creates the directory for storing build logs if it doesn't exist.
     */
    private void createLogDirectory() {
        Path logDirPath = Paths.get(StringConstants.BUILD_LOGS_DIRECTORY);
        if (!Files.exists(logDirPath)) {
            try {
                Files.createDirectories(logDirPath);
                log.info("Created build log directory: {}", logDirPath.toAbsolutePath());
            } catch (IOException e) {
                log.error("Failed to create build log directory: {}",
                        logDirPath.toAbsolutePath(), e);
            }
        }
    }

    /**
     * Initiates the asynchronous generation of a log file for a specific build.
     * Validates the build identifier, creates a task ID, stores initial status in cache,
     * and launches the background generation task.
     *
     * @param identifier The ID or exact name of the build.
     * @return A unique task ID (UUID string) for tracking the generation process.
     * @throws NoSuchElementException if the build identified by the identifier is not found.
     */
    public String initiateLogGeneration(String identifier) {
        Build buildStub = findBuildByIdentifier(identifier);
        Long buildId = buildStub.getId();
        String taskId = UUID.randomUUID().toString();
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);

        TaskStatusDto initialStatus = TaskStatusDto.pending(taskId);
        cache.put(cacheKey, initialStatus);
        log.info("Initiating log generation for build ID {} with task ID: {}", buildId, taskId);

        // Call the async/transactional method via the injected 'self' proxy
        CompletableFuture.runAsync(() -> self.performLogGeneration(buildId, taskId), taskExecutor)
                .exceptionally(ex -> {
                    log.error("Unhandled exception wrapper caught for async task ID {}: {}",
                            taskId, ex.getMessage(), ex);
                    Optional<TaskStatusDto> currentStatus = getTaskStatus(taskId);
                    if (currentStatus.isEmpty()
                            || currentStatus.get().status() != TaskState.FAILED) {
                        updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                                "Unexpected error during async execution wrapper."));
                    }
                    return null;
                });

        return taskId;
    }

    /**
     * Performs the actual log file generation asynchronously within a transaction.
     * Fetches the Build with associations eagerly loaded (excluding LOBs), introduces an
     * artificial delay, builds the log content (lazily loading LOB if needed),
     * and writes the log file. Updates task status in cache upon completion or failure.
     *
     * <p>This method is intended to be called via Spring's {@code @Async} mechanism,
     * typically through the self-injected proxy.
     *
     * @param buildId The ID of the build to generate the log for.
     * @param taskId  The unique task ID for status updates.
     */
    @Async("taskExecutor")
    @Transactional(readOnly = true) // Ensures session for lazy loading LOB
    public void performLogGeneration(Long buildId, String taskId) {
        log.info("Starting async log generation for build ID {}, task ID {}", buildId, taskId);

        try {
            log.info("Task ID {}: Introducing artificial delay of {} ms...",
                    taskId, ARTIFICIAL_DELAY_MS);
            Thread.sleep(ARTIFICIAL_DELAY_MS);
            log.info("Task ID {}: Artificial delay finished.", taskId);
        } catch (InterruptedException e) {
            log.warn("Task ID {}: Log generation task interrupted during artificial delay.",
                    taskId);
            Thread.currentThread().interrupt();
            updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                    "Task interrupted during processing."));
            return;
        }

        try {
            Optional<Build> buildOpt = buildService.findBuildFullyLoadedForLog(buildId);

            if (buildOpt.isEmpty()) {
                log.error("Build ID {} not found (with full load attempt) for task ID {}.",
                        buildId, taskId);
                updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                        "Build not found during generation."));
                return;
            }
            Build build = buildOpt.get();

            log.info("Task ID {}: Building log content...", taskId);
            String logContent = buildLogContent(build);

            Path logFilePath = Paths.get(StringConstants.BUILD_LOGS_DIRECTORY,
                    String.format(StringConstants.BUILD_LOG_FILENAME_TEMPLATE, buildId));

            writeLogToFile(taskId, logFilePath, logContent);

            updateTaskStatus(taskId, TaskStatusDto.completed(taskId, logFilePath.toString()));

        } catch (IOException ioe) {
            log.error("IOException while writing log file for task ID {}: {}",
                    taskId, ioe.getMessage(), ioe);
            updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                    "Failed to write log file: " + ioe.getMessage()));
        } catch (LazyInitializationException lie) {
            log.error("LazyInitializationException likely accessing schemFile for task ID {}: {}",
                    taskId, lie.getMessage());
            updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                    "Internal error accessing build data (LOB)."));
        } catch (Exception e) {
            // Catch any other unexpected errors
            log.error("Unexpected error during log generation for task ID {}: {}",
                    taskId, e.getMessage(), e);
            updateTaskStatus(taskId, TaskStatusDto.failed(taskId,
                    "Internal error during log generation: " + e.getClass().getSimpleName()));
            // No re-throw here, allow task to complete with FAILED status
        }
    }

    /**
     * Writes the generated log content to the specified file path.
     *
     * @param taskId      The task ID for logging purposes.
     * @param logFilePath The Path object representing the log file.
     * @param logContent  The string content to write.
     * @throws IOException if an I/O error occurs during writing.
     */
    private void writeLogToFile(String taskId, Path logFilePath, String logContent)
            throws IOException {
        log.info("Task ID {}: Writing log file to {}...", taskId, logFilePath.toAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(logFilePath)) {
            writer.write(logContent);
            log.info("Successfully generated log file for task ID {} at: {}",
                    taskId, logFilePath.toAbsolutePath());
        }
    }


    /**
     * Retrieves the current status of a log generation task from the cache.
     *
     * @param taskId The unique ID of the task.
     * @return An {@code Optional<TaskStatusDto>} containing the status if found,
     *         otherwise {@code Optional.empty()}.
     */
    public Optional<TaskStatusDto> getTaskStatus(String taskId) {
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);
        return cache.get(cacheKey);
    }

    /**
     * Builds the string content for the build log file.
     * Assumes the necessary associations (collections) on the Build object are loaded.
     * Attempts to lazily load the schematic file size.
     *
     * @param build The Build object (potentially with eagerly loaded collections).
     * @return The formatted string content for the log file.
     */
    private String buildLogContent(Build build) {
        StringBuilder sb = new StringBuilder();
        String generatedTimestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        sb.append("Minecraft Build Log\n");
        sb.append(LOG_SEPARATOR);
        sb.append("Generated: ").append(generatedTimestamp).append(LOG_LINE_BREAK)
                .append(LOG_LINE_BREAK);

        sb.append("Build ID: ").append(build.getId()).append(LOG_LINE_BREAK);
        sb.append("Build Name: ").append(build.getName()).append(LOG_LINE_BREAK)
                .append(LOG_LINE_BREAK);

        appendCollectionDetails(sb, "Authors", build.getAuthors(), Author::getName);
        appendCollectionDetails(sb, "Themes", build.getThemes(), Theme::getName);
        appendCollectionDetails(sb, "Colors", build.getColors(), Color::getName);
        appendDescription(sb, build.getDescription());
        appendScreenshots(sb, build.getScreenshots());
        appendSchematicInfo(sb, build);

        sb.append(LOG_SEPARATOR);
        sb.append("End of Log\n");

        return sb.toString();
    }

    /** Appends details for a collection of BaseNamedEntity items. */
    private <T extends BaseNamedEntity> void appendCollectionDetails(StringBuilder sb,
                                                                     String sectionTitle,
                                                                     Collection<T> items,
                                                                     Function<T, String>
                                                                             nameExtractor) {
        sb.append(sectionTitle).append(":\n");
        if (items != null && !items.isEmpty()) {
            items.stream()
                    .sorted(Comparator.comparing(nameExtractor))
                    .forEach(item -> sb.append(LOG_PREFIX_ID).append(item.getId())
                            .append(LOG_PREFIX_NAME).append(item.getName())
                            .append(LOG_LINE_BREAK));
        } else {
            sb.append(LOG_NONE);
        }
        sb.append(LOG_LINE_BREAK);
    }

    /** Appends the description section. */
    private void appendDescription(StringBuilder sb, String description) {
        sb.append("Description:\n");
        sb.append(description != null && !description.isBlank()
                        ? description : LOG_NONE.stripTrailing())
                .append(LOG_LINE_BREAK).append(LOG_LINE_BREAK);
    }

    /** Appends the screenshots section. */
    private void appendScreenshots(StringBuilder sb, List<String> screenshots) {
        sb.append("Screenshots:\n");
        if (screenshots != null && !screenshots.isEmpty()) {
            screenshots.stream().sorted()
                    .forEach(screenshot -> sb.append(LOG_INDENT).append("- ")
                            .append(screenshot).append(LOG_LINE_BREAK));
        } else {
            sb.append(LOG_NONE);
        }
        sb.append(LOG_LINE_BREAK);
    }

    /** Appends the schematic file information section. */
    private void appendSchematicInfo(StringBuilder sb, Build build) {
        sb.append("Schematic File:\n");
        byte[] schemBytes;
        try {
            schemBytes = build.getSchemFile();
            if (schemBytes != null) {
                sb.append(LOG_INDENT).append("Size: ").append(schemBytes.length)
                        .append(" bytes\n");
            } else {
                sb.append(LOG_INDENT).append("(Not present)\n");
            }
        } catch (LazyInitializationException lie) {
            log.warn("Could not lazy-load schemFile within buildLogContent: {}", lie.getMessage());
            sb.append(LOG_INDENT).append("(Error loading file data)\n");
        } catch (Exception e) {
            log.error("Error accessing schemFile data: {}", e.getMessage(), e);
            sb.append(LOG_INDENT).append("(Error accessing file data)\n");
        }
        sb.append(LOG_LINE_BREAK);
    }


    /**
     * Updates the status of a log generation task in the cache.
     *
     * @param taskId    The ID of the task to update.
     * @param statusDto The new status DTO to store.
     */
    private void updateTaskStatus(String taskId, TaskStatusDto statusDto) {
        String cacheKey = InMemoryCache.generateKey(StringConstants.LOG_TASK_CACHE_PREFIX, taskId);
        cache.put(cacheKey, statusDto);
        log.info("Updated status for task ID {} to: {}", taskId, statusDto.status());
    }

    /**
     * Finds a build by its identifier (ID or name) using the BuildService.
     *
     * @param identifier The ID or exact name of the build.
     * @return The found Build object.
     * @throws NoSuchElementException if the build is not found.
     */
    private Build findBuildByIdentifier(String identifier) {
        return buildService.findBuildByIdentifier(identifier);
    }
}