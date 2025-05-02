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
        registry.addInterceptor(visitCounterInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/visit-count",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/webjars/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
            );
    }
}