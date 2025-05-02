package sovok.mcbuildlibrary.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class VisitCounterService {

    // Renamed to reflect total requests
    private final AtomicInteger totalRequestCount = new AtomicInteger(0);

    /**
     * Increments the total request count atomically.
     *
     * @return The new count after incrementing.
     */
    public int incrementTotalRequests() {
        return totalRequestCount.incrementAndGet();
    }

    /**
     * Gets the current total request count.
     *
     * @return The current count.
     */
    public int getTotalRequestCount() {
        return totalRequestCount.get();
    }
}