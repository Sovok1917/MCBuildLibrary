// file: src/main/java/sovok/mcbuildlibrary/interceptor/VisitCounterInterceptor.java
package sovok.mcbuildlibrary.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sovok.mcbuildlibrary.service.VisitCounterService;

@Component
public class VisitCounterInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(VisitCounterInterceptor.class);
    // No longer need TARGET_URL

    private final VisitCounterService visitCounterService;

    @Autowired
    public VisitCounterInterceptor(VisitCounterService visitCounterService) {
        this.visitCounterService = visitCounterService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Increment for every request intercepted by this interceptor's path patterns
        int newCount = visitCounterService.incrementTotalRequests();
        // Log the URI for context
        log.debug("Intercepted request for URI: {}. Incremented total count to: {}", request.getRequestURI(), newCount);

        // Allow the request to proceed
        return true;
    }
}