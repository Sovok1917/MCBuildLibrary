package sovok.mcbuildlibrary.config;

import java.util.concurrent.Executor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync // Can also be placed here instead of Application.java, but Application.java is common
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        log.debug("Creating Async Task Executor");
        final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Configure core/max pool size based on expected load and system resources
        executor.setCorePoolSize(5); // Start with 5 threads
        executor.setMaxPoolSize(10); // Allow up to 10 threads
        executor.setQueueCapacity(25); // Queue tasks if all threads are busy
        executor.setThreadNamePrefix("BuildLogGen-");
        executor.initialize();
        log.info("Configured ThreadPoolTaskExecutor with CorePoolSize={}, MaxPoolSize={},"
                        + " QueueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
        return executor;
    }
}