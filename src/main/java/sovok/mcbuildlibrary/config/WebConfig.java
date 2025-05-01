package sovok.mcbuildlibrary.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sovok.mcbuildlibrary.interceptor.VisitCounterInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final VisitCounterInterceptor visitCounterInterceptor;

    @Autowired
    public WebConfig(VisitCounterInterceptor visitCounterInterceptor) {
        this.visitCounterInterceptor = visitCounterInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Register the interceptor to apply to all paths...
        registry.addInterceptor(visitCounterInterceptor)
                .addPathPatterns("/**") // Apply to all paths initially
                // ...but exclude specific paths
                .excludePathPatterns(
                        "/visit-count",         // Don't count requests to the counter endpoint itself
                        "/error",               // Don't count Spring Boot error dispatches
                        "/css/**",              // Exclude static resources
                        "/js/**",
                        "/images/**",
                        "/webjars/**",          // Exclude webjars (used by Swagger UI etc.)
                        "/swagger-ui.html",     // Exclude Swagger UI HTML page
                        "/swagger-ui/**",       // Exclude Swagger UI resources
                        "/v3/api-docs",         // Exclude OpenAPI spec endpoint
                        "/v3/api-docs/**"       // Exclude OpenAPI spec resources
                        // Add any other paths you want to exclude
                );
    }
}