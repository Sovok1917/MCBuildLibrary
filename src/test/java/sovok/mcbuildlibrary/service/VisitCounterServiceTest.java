// file: src/test/java/sovok/mcbuildlibrary/service/VisitCounterServiceTest.java
package sovok.mcbuildlibrary.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link VisitCounterService}. Verifies initial state, increment logic,
 * and thread safety using AtomicInteger.
 */
class VisitCounterServiceTest {

    private static final int THREAD_COUNT = 20;
    private static final int INCREMENTS_PER_THREAD = 500;
    private static final int LATCH_TIMEOUT_SECONDS = 10;

    private VisitCounterService visitCounterService;

    @BeforeEach
    void setUp() {
        visitCounterService = new VisitCounterService();
    }

    @Test
    @DisplayName("getTotalRequestCount: Initial State - Should return zero")
    void getTotalRequestCount_initial_shouldBeZero() {
        // Act
        int count = visitCounterService.getTotalRequestCount();

        // Assert
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("incrementTotalRequests: Single Thread - Should increment count correctly")
    void incrementTotalRequests_shouldIncrementCount() {
        // Act
        int count1 = visitCounterService.incrementTotalRequests();
        int count2 = visitCounterService.incrementTotalRequests();
        int count3 = visitCounterService.incrementTotalRequests();
        int finalCount = visitCounterService.getTotalRequestCount();

        // Assert
        assertThat(count1).isEqualTo(1);
        assertThat(count2).isEqualTo(2);
        assertThat(count3).isEqualTo(3);
        assertThat(finalCount).isEqualTo(3);
    }

    @Test
    @DisplayName("incrementTotalRequests: Multi Thread - Should be thread-safe")
    void incrementTotalRequests_shouldBeThreadSafe() throws InterruptedException {
        // Arrange
        int expectedTotal = THREAD_COUNT * INCREMENTS_PER_THREAD;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);

        // Act: Submit tasks to increment the counter concurrently
        for (int i = 0; i < THREAD_COUNT; i++) {
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
                        visitCounterService.incrementTotalRequests();
                    }
                } finally {
                    latch.countDown(); // Signal completion for this thread
                }
            });
        }

        // Wait for all threads to complete, with a timeout
        boolean completed = latch.await(LATCH_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        executorService.shutdown(); // Initiate shutdown

        // Assert
        assertThat(completed).as("All threads should complete within the timeout").isTrue();
        assertThat(visitCounterService.getTotalRequestCount())
                .as("Final count should equal the total expected increments")
                .isEqualTo(expectedTotal);
    }
}