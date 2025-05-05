package sovok.mcbuildlibrary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sovok.mcbuildlibrary.TestConstants.AUTHOR_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.AUTHOR_NAME_2;
import static sovok.mcbuildlibrary.TestConstants.BUILD_DESC_1;
import static sovok.mcbuildlibrary.TestConstants.BUILD_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.COLOR_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.TEST_ID_1;
import static sovok.mcbuildlibrary.TestConstants.TEST_SCHEM_BYTES;
import static sovok.mcbuildlibrary.TestConstants.THEME_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.createTestAuthor;
import static sovok.mcbuildlibrary.TestConstants.createTestBuild;
import static sovok.mcbuildlibrary.TestConstants.createTestColor;
import static sovok.mcbuildlibrary.TestConstants.createTestTheme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections; // For empty collections
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.TaskState;
import sovok.mcbuildlibrary.dto.TaskStatusDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;

/**
 * Unit tests for the {@link BuildLogService}. Focuses on task initiation, status tracking,
 * asynchronous execution logic (mocked), file writing interactions, log content generation,
 * and error handling.
 */
@ExtendWith(MockitoExtension.class)
class BuildLogServiceTest {

    private static final long VERIFY_TIMEOUT_MS = 5000; // Timeout for async verification
    private static final String LOG_TASK_PREFIX = StringConstants.LOG_TASK_CACHE_PREFIX;

    @Mock
    private BuildService buildService;
    @Mock
    private InMemoryCache cache;
    @Mock
    private Executor taskExecutor;

    @InjectMocks
    private BuildLogService buildLogService;

    // Spy for self-invocation testing and protected method verification
    private BuildLogService buildLogServiceSpy;

    @Captor
    private ArgumentCaptor<TaskStatusDto> taskStatusCaptor;
    @Captor
    private ArgumentCaptor<String> cacheKeyCaptor;
    @Captor
    private ArgumentCaptor<Runnable> runnableCaptor;
    @Captor
    private ArgumentCaptor<String> logContentCaptor; // For verifying log content

    private Build testBuild;
    private String testBuildIdentifier;

    @BeforeEach
    void setUp() throws IOException {
        // Explicitly reset mocks before each test to prevent state leakage
        reset(cache, buildService, taskExecutor);

        // Setup standard test build
        Author author1 = createTestAuthor(1L, AUTHOR_NAME_1);
        Theme theme1 = createTestTheme(1L, THEME_NAME_1);
        Color color1 = createTestColor(1L, COLOR_NAME_1);
        testBuild = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(author1), Set.of(theme1),
                Set.of(color1));
        testBuild.setDescription(BUILD_DESC_1);
        testBuild.setSchemFile(TEST_SCHEM_BYTES);
        testBuild.setScreenshots(List.of("screen1.png", "screen2.jpg"));
        testBuildIdentifier = String.valueOf(TEST_ID_1);

        // Calculate expected log directory path based on constants
        Path expectedLogDirectoryPath = Paths.get(StringConstants.BUILD_LOGS_DIRECTORY);

        // Create spy and inject AFTER resetting mocks
        buildLogServiceSpy = spy(buildLogService);
        ReflectionTestUtils.setField(buildLogService, "self", buildLogServiceSpy);

        // Ensure the target directory exists for tests that might write files
        Files.createDirectories(expectedLogDirectoryPath);
    }

    @AfterEach
    void tearDown() {
        // Clean up resources if necessary (e.g., shutdown real executors)
        if (taskExecutor instanceof ThreadPoolTaskExecutor) {
            ((ThreadPoolTaskExecutor) taskExecutor).shutdown();
        }
    }

    // --- Helper Method for Log Path ---
    private String getExpectedLogFilePathString() {
        String filename = String.format(StringConstants.BUILD_LOG_FILENAME_TEMPLATE, sovok.mcbuildlibrary.TestConstants.TEST_ID_1);
        return Paths.get(StringConstants.BUILD_LOGS_DIRECTORY, filename).toString();
    }

    // =========================================================================
    // initiateLogGeneration Tests
    // =========================================================================

    @Test
    @DisplayName("initiateLogGeneration: Success - Should cache PENDING and submit task")
    void initiateLogGeneration_shouldCachePendingAndSubmitTask() {
        // Arrange
        when(buildService.findBuildByIdentifier(testBuildIdentifier)).thenReturn(testBuild);
        doAnswer(invocation -> CompletableFuture.completedFuture(null))
                .when(taskExecutor).execute(any(Runnable.class));

        // Act
        String taskId = buildLogService.initiateLogGeneration(testBuildIdentifier);

        // Assert
        assertThat(taskId).isNotNull();
        assertThat(UUID.fromString(taskId)).isNotNull();
        verify(buildService).findBuildByIdentifier(testBuildIdentifier);
        verify(cache, times(1)).put(cacheKeyCaptor.capture(), taskStatusCaptor.capture());
        assertThat(cacheKeyCaptor.getValue()).isEqualTo(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId));
        TaskStatusDto initialStatus = taskStatusCaptor.getValue();
        assertThat(initialStatus.status()).isEqualTo(TaskState.PENDING);
        assertThat(initialStatus.taskId()).isEqualTo(taskId);
        verify(taskExecutor).execute(runnableCaptor.capture());
        assertThat(runnableCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("initiateLogGeneration: Failure - Should throw NoSuchElementException if build not found")
    void initiateLogGeneration_whenBuildNotFound_shouldThrowException() {
        // Arrange
        String invalidIdentifier = "invalid-999";
        when(buildService.findBuildByIdentifier(invalidIdentifier))
                .thenThrow(new NoSuchElementException("Build not found"));

        // Act & Assert
        assertThatThrownBy(() -> buildLogService.initiateLogGeneration(invalidIdentifier))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Build not found");
        verify(cache, never()).put(anyString(), any(TaskStatusDto.class));
        verify(taskExecutor, never()).execute(any());
    }

    @Test
    @DisplayName("initiateLogGeneration: Failure - Should throw RejectedExecutionException if executor rejects")
    void initiateLogGeneration_whenExecutorRejects_shouldThrowAndCachePending() {
        // Arrange
        String expectedExceptionMessage = "Executor task queue is full";
        when(buildService.findBuildByIdentifier(testBuildIdentifier)).thenReturn(testBuild);
        doThrow(new RejectedExecutionException(expectedExceptionMessage))
                .when(taskExecutor).execute(any(Runnable.class));

        // Act & Assert
        RejectedExecutionException thrownException =
                Assertions.assertThrows(
                        RejectedExecutionException.class,
                        () -> buildLogService.initiateLogGeneration(testBuildIdentifier),
                        "Expected initiateLogGeneration to throw RejectedExecutionException");
        assertThat(thrownException.getMessage()).contains(expectedExceptionMessage);

        // --- Verifications AFTER the exception is caught ---
        verify(cache, times(1)).put(cacheKeyCaptor.capture(), taskStatusCaptor.capture());
        String capturedCacheKey = cacheKeyCaptor.getValue();
        TaskStatusDto initialStatus = taskStatusCaptor.getValue();
        String expectedPrefix = InMemoryCache.generateKey(LOG_TASK_PREFIX, "");
        assertThat(capturedCacheKey).startsWith(expectedPrefix);
        String taskId = capturedCacheKey.substring(expectedPrefix.length());
        try {
            assertThat(UUID.fromString(taskId)).isNotNull();
        } catch (IllegalArgumentException e) {
            Assertions.fail("Captured cache key suffix is not a valid UUID: " + taskId);
        }
        assertThat(initialStatus.status()).isEqualTo(TaskState.PENDING);
        assertThat(initialStatus.taskId()).isEqualTo(taskId);
        verify(taskExecutor, times(1)).execute(any(Runnable.class));
        verify(cache, times(1)).put(anyString(), any(TaskStatusDto.class)); // Only PENDING put
    }

    @Test
    @DisplayName("initiateLogGeneration: Failure - Should handle async task failure via exceptionally")
    void initiateLogGeneration_whenAsyncTaskFails_shouldUpdateStatusToFailed() {
        // Arrange
        String errorMessage = "Error during async processing";
        String taskId; // Will be set during execution or verification

        when(buildService.findBuildByIdentifier(testBuildIdentifier)).thenReturn(testBuild);

        // Mock the executor to immediately execute the runnable
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            // We need the taskId generated *before* the task runs to mock the correct call
            // Capture the taskId from the cache put that happens *before* execute
            ArgumentCaptor<String> keyCaptorForId = ArgumentCaptor.forClass(String.class);
            verify(cache).put(keyCaptorForId.capture(), any(TaskStatusDto.class));
            String capturedKey = keyCaptorForId.getValue();
            String extractedTaskId = capturedKey.substring(InMemoryCache.generateKey(LOG_TASK_PREFIX, "").length());

            // Now mock the spy's method call with the correct taskId
            doThrow(new RuntimeException(errorMessage))
                    .when(buildLogServiceSpy).performLogGeneration(TEST_ID_1, extractedTaskId);

            // Run the task (which will now throw inside the CompletableFuture)
            task.run();
            return CompletableFuture.completedFuture(null); // Outer submission succeeds
        }).when(taskExecutor).execute(any(Runnable.class));

        // Act
        // Call the method. We expect it to complete without throwing here,
        // as the exception is handled by .exceptionally()
        taskId = buildLogService.initiateLogGeneration(testBuildIdentifier);
        assertThat(taskId).isNotNull(); // Ensure taskId was generated

        // Assert
        // Verify the FAILED status update from the .exceptionally block occurred.
        // We expect exactly TWO puts in total (PENDING then FAILED).
        // The timeout might not be strictly needed due to synchronous mock execution, but adds robustness.
        verify(cache, timeout(VERIFY_TIMEOUT_MS).times(2)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)), // Use the generated taskId
                taskStatusCaptor.capture());

        List<TaskStatusDto> statuses = taskStatusCaptor.getAllValues();
        assertThat(statuses).hasSize(2);

        // Assert the first status was PENDING
        TaskStatusDto pendingStatus = statuses.get(0);
        assertThat(pendingStatus.status()).isEqualTo(TaskState.PENDING);
        assertThat(pendingStatus.taskId()).isEqualTo(taskId);
        assertThat(pendingStatus.errorMessage()).isNull();

        // Assert the second status was FAILED
        TaskStatusDto failedStatus = statuses.get(1);
        assertThat(failedStatus.status()).isEqualTo(TaskState.FAILED);
        assertThat(failedStatus.taskId()).isEqualTo(taskId);
        // Check the specific error message set by the exceptionally block
        assertThat(failedStatus.errorMessage())
                .contains("Unexpected error during async execution wrapper.");

        // Verify the mocked async method was called
        verify(buildLogServiceSpy).performLogGeneration(TEST_ID_1, taskId);
    }

    // =========================================================================
    // performLogGeneration Tests (including log content)
    // =========================================================================

    @Test
    @DisplayName("performLogGeneration: Success - Should write detailed log and cache COMPLETED")
    void performLogGeneration_onSuccess_shouldWriteLogAndCacheCompleted() throws Exception {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String expectedFilePathString = getExpectedLogFilePathString();
        // Add a second author for sorting verification
        Author author2 = createTestAuthor(2L, AUTHOR_NAME_2); // Assume AUTHOR_NAME_2 sorts after AUTHOR_NAME_1
        testBuild.setAuthors(Set.of(author2, createTestAuthor(1L, AUTHOR_NAME_1))); // Unordered set

        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(testBuild));

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(buildLogServiceSpy, times(1)).writeLogToFile(
                eq(taskId), any(Path.class), logContentCaptor.capture());

        // --- Detailed Log Content Verification ---
        String logContent = logContentCaptor.getValue();
        assertThat(logContent)
                .contains("Minecraft Build Log")
                .contains("=====================")
                .contains("Generated: ")
                .contains("Build ID: " + TEST_ID_1)
                .contains("Build Name: " + BUILD_NAME_1)
                .contains("Authors:")
                // Check for sorted authors
                .contains("  - ID: 1, Name: " + AUTHOR_NAME_1)
                .contains("  - ID: 2, Name: " + AUTHOR_NAME_2)
                .contains("Themes:")
                .contains("  - ID: 1, Name: " + THEME_NAME_1)
                .contains("Colors:")
                .contains("  - ID: 1, Name: " + COLOR_NAME_1)
                .contains("Description:")
                .contains("  " + BUILD_DESC_1) // Check indentation
                .contains("Screenshots:")
                .contains("  - screen1.png") // Check indentation and sorting
                .contains("  - screen2.jpg")
                .contains("Schematic File:")
                .contains("  Size: " + TEST_SCHEM_BYTES.length + " bytes") // Check indentation
                .contains("End of Log");

        // Check author sorting explicitly
        int author1Index = logContent.indexOf(AUTHOR_NAME_1);
        int author2Index = logContent.indexOf(AUTHOR_NAME_2);
        assertThat(author1Index).isLessThan(author2Index);

        // Verify final status update to COMPLETED
        verify(cache, times(1)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.COMPLETED);
        assertThat(finalStatus.filePath()).isEqualTo(expectedFilePathString);
        assertThat(finalStatus.errorMessage()).isNull();
    }

    @Test
    @DisplayName("performLogGeneration: Success - Should handle null/empty fields gracefully in log")
    void performLogGeneration_withNullOrEmptyFields_shouldLogPlaceholders() throws Exception {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String expectedFilePathString = getExpectedLogFilePathString();
        // Modify build to have empty/null fields
        testBuild.setAuthors(Collections.emptySet()); // Empty set
        testBuild.setThemes(null); // Null collection
        testBuild.setColors(Set.of(createTestColor(1L, COLOR_NAME_1))); // Keep one
        testBuild.setDescription(""); // Blank description
        testBuild.setScreenshots(List.of()); // Empty list
        testBuild.setSchemFile(null); // Null schematic data

        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(testBuild));

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(buildLogServiceSpy, times(1)).writeLogToFile(
                eq(taskId), any(Path.class), logContentCaptor.capture());

        // --- Log Content Verification for Placeholders ---
        String logContent = logContentCaptor.getValue();
        assertThat(logContent)
                .contains("Authors:\n  (None)\n") // Check placeholder for empty set
                .contains("Themes:\n  (None)\n") // Check placeholder for null collection
                .contains("Colors:\n  - ID: 1, Name: " + COLOR_NAME_1) // Check non-empty collection
                .contains("Description:\n  (None)\n") // Check placeholder for blank description
                .contains("Screenshots:\n  (None)\n") // Check placeholder for empty list
                .contains("Schematic File:\n  (Not present)\n"); // Check placeholder for null data

        // Verify final status update to COMPLETED
        verify(cache, times(1)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.COMPLETED);
        assertThat(finalStatus.filePath()).isEqualTo(expectedFilePathString);
    }


    @Test
    @DisplayName("performLogGeneration: Failure - Should cache FAILED if build not found")
    void performLogGeneration_whenBuildNotFound_shouldCacheFailed() throws IOException {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.empty());

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(buildLogServiceSpy, never()).writeLogToFile(anyString(), any(Path.class), anyString());
        verify(cache, times(1)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.FAILED);
        assertThat(finalStatus.errorMessage()).contains("Build not found during generation.");
    }

    @Test
    @DisplayName("performLogGeneration: Failure - Should cache FAILED on IOException during write")
    void performLogGeneration_onIOException_shouldCacheFailed() throws IOException {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String ioErrorMessage = "Disk is critically full";
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(testBuild));
        doThrow(new IOException(ioErrorMessage)).when(buildLogServiceSpy)
                .writeLogToFile(eq(taskId), any(Path.class), anyString());

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(buildLogServiceSpy).writeLogToFile(eq(taskId), any(Path.class), anyString());
        verify(cache, times(1)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.FAILED);
        assertThat(finalStatus.errorMessage()).contains("Failed to write log file: " + ioErrorMessage);
    }

    @Test
    @DisplayName("performLogGeneration: Success - Handles LazyInitException gracefully")
    void performLogGeneration_onLazyInitException_shouldCompleteGracefully() throws IOException {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String expectedFilePathString = getExpectedLogFilePathString();
        Build buildMock = mock(Build.class); // Use mock for precise control

        when(buildMock.getId()).thenReturn(TEST_ID_1);
        when(buildMock.getName()).thenReturn(BUILD_NAME_1);
        // Authors not strictly needed for this specific path, but good practice
        when(buildMock.getAuthors()).thenReturn(Set.of(createTestAuthor(1L, AUTHOR_NAME_1)));
        // Simulate LazyInitializationException when accessing schemFile
        when(buildMock.getSchemFile()).thenThrow(new LazyInitializationException("Session closed"));
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(buildMock));

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1); // Verify interaction

        // Verify writeLogToFile was called and capture its arguments
        verify(buildLogServiceSpy, times(1)).writeLogToFile(
                eq(taskId), any(Path.class), logContentCaptor.capture());

        // Assert on the captured log content using chained assertions
        assertThat(logContentCaptor.getValue())
                .contains("Schematic File:")
                .contains("(Error loading file data - LazyInit)"); // Chained assertion

        // Verify cache interaction and capture the status DTO
        verify(cache, times(1)).put(
                eq(InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId)),
                taskStatusCaptor.capture());

        // Assert on multiple properties of the captured TaskStatusDto using extracting()
        // Assumes TaskStatusDto has public getters: status(), filePath(), errorMessage()
        // If they are fields, use strings: .extracting("status", "filePath", "errorMessage")
        assertThat(taskStatusCaptor.getValue())
                .extracting(TaskStatusDto::status, TaskStatusDto::filePath, TaskStatusDto::errorMessage)
                .containsExactly(TaskState.COMPLETED, expectedFilePathString, null);
    }

    @Test
    @DisplayName("performLogGeneration: Failure - Should cache FAILED on RuntimeException during build load")
    void performLogGeneration_onRuntimeExceptionDuringLoad_shouldCacheFailedOnce() throws IOException {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String errorMessage = "Simulated DB error during load";
        String expectedCacheKey = InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId);
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1))
                .thenThrow(new RuntimeException(errorMessage));

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(cache, times(1)).put( // Should be called exactly ONCE
                eq(expectedCacheKey),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.FAILED);
        assertThat(finalStatus.taskId()).isEqualTo(taskId);
        assertThat(finalStatus.errorMessage())
                .isEqualTo("Internal error during log generation: RuntimeException");
        verify(buildLogServiceSpy, never()).writeLogToFile(anyString(), any(Path.class), anyString());
    }

    @Test
    @DisplayName("performLogGeneration: Failure - Should cache FAILED on Exception during content build")
    void performLogGeneration_onExceptionDuringContentBuild_shouldCacheFailedOnce() throws IOException {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String errorMessage = "Error during content generation";
        String expectedCacheKey = InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId);

        // Mock build service to return the build successfully
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(testBuild));
        // Mock the internal buildLogContent call (via spy) to throw an exception
        // Note: This requires buildLogContent to be non-private or use PowerMock/ReflectionTestUtils
        // Since it's private, we'll mock a method called *within* buildLogContent, e.g., getName()
        Build buildSpy = spy(testBuild);
        when(buildSpy.getName()).thenThrow(new RuntimeException(errorMessage)); // Simulate error point
        when(buildService.findBuildFullyLoadedForLog(TEST_ID_1)).thenReturn(Optional.of(buildSpy));

        // Act
        buildLogServiceSpy.performLogGeneration(TEST_ID_1, taskId);

        // Assert
        verify(buildService).findBuildFullyLoadedForLog(TEST_ID_1);
        verify(cache, times(1)).put( // Should be called exactly ONCE
                eq(expectedCacheKey),
                taskStatusCaptor.capture());
        TaskStatusDto finalStatus = taskStatusCaptor.getValue();
        assertThat(finalStatus.status()).isEqualTo(TaskState.FAILED);
        assertThat(finalStatus.taskId()).isEqualTo(taskId);
        assertThat(finalStatus.errorMessage())
                .isEqualTo("Internal error during log generation: RuntimeException"); // Caught by generic catch
        verify(buildLogServiceSpy, never()).writeLogToFile(anyString(), any(Path.class), anyString());
    }

    // =========================================================================
    // getTaskStatus Tests
    // =========================================================================

    @Test
    @DisplayName("getTaskStatus: Success - Should return status from cache when exists")
    void getTaskStatus_whenExists_shouldReturnStatus() {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String cacheKey = InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId);
        String filePath = getExpectedLogFilePathString();
        TaskStatusDto expectedStatus = TaskStatusDto.completed(taskId, filePath);
        when(cache.get(cacheKey)).thenReturn(Optional.of(expectedStatus));

        // Act
        Optional<TaskStatusDto> actualStatus = buildLogService.getTaskStatus(taskId);

        // Assert
        assertThat(actualStatus).isPresent().contains(expectedStatus);
        verify(cache).get(cacheKey);
    }

    @Test
    @DisplayName("getTaskStatus: Not Found - Should return empty when status not in cache")
    void getTaskStatus_whenNotExists_shouldReturnEmpty() {
        // Arrange
        String taskId = UUID.randomUUID().toString();
        String cacheKey = InMemoryCache.generateKey(LOG_TASK_PREFIX, taskId);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());

        // Act
        Optional<TaskStatusDto> actualStatus = buildLogService.getTaskStatus(taskId);

        // Assert
        assertThat(actualStatus).isEmpty();
        verify(cache).get(cacheKey);
    }
}