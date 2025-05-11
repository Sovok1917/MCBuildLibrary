package sovok.mcbuildlibrary.service;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

@Service
public class VisitCounterService {

    private final AtomicInteger totalRequestCount = new AtomicInteger(0);

    public int incrementTotalRequests() {
        return totalRequestCount.incrementAndGet();
    }

    public int getTotalRequestCount() {
        return totalRequestCount.get();
    }
}