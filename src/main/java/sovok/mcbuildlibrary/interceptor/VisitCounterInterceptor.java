package sovok.mcbuildlibrary.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull; // Import NonNull
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sovok.mcbuildlibrary.service.VisitCounterService;

/**
 * Intercepts incoming requests to count the total number of requests handled by controllers.
 * Excludes specific paths like static resources, error pages, and Swagger UI.
 */
@Component
public class VisitCounterInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VisitCounterInterceptor.class);

    private final VisitCounterService visitCounterService;

    /**
     * Constructs the interceptor with the required VisitCounterService.
     *
     * @param visitCounterService The service used to track visit counts.
     */
    @Autowired
    public VisitCounterInterceptor(VisitCounterService visitCounterService) {
        this.visitCounterService = visitCounterService;
    }

    /**
     * Intercepts the request before it reaches the handler method. Increments the total request
     * count for requests matching the configured path patterns.
     *
     * @param request  The current HTTP request.
     * @param response The current HTTP response.
     * @param handler  The chosen handler to execute, for type and/or instance examination.
     * @return {@code true} if the execution chain should proceed with the next interceptor or the
     *         handler itself. Else, DispatcherServlet assumes that this interceptor
     *         has already dealt
     *         with the response itself.
     */
    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request, // Add @NonNull
            @NonNull HttpServletResponse response, // Add @NonNull
            @NonNull Object handler) { // Add @NonNull, remove 'throws Exception'

        // Increment for every request intercepted by this interceptor's path patterns
        int newCount = visitCounterService.incrementTotalRequests();
        // Log the URI for context (split log statement for line length)
        log.debug("Intercepted request for URI: {}. Incremented total count to: {}",
                request.getRequestURI(), newCount);

        // Allow the request to proceed
        return true;
    }
}