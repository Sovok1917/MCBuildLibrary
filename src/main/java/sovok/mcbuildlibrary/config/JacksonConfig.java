package sovok.mcbuildlibrary.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * Configuration for Jackson serialization, specifically for Spring Data Page instances.
 */
@Configuration
@EnableSpringDataWebSupport(
    pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class JacksonConfig {
}