package sovok.mcbuildlibrary;

import io.swagger.v3.oas.annotations.OpenAPIDefinition; // Import
import io.swagger.v3.oas.annotations.info.Info;         // Import
import io.swagger.v3.oas.annotations.info.License;     // Import
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@OpenAPIDefinition(// Add OpenAPI Definition
        info = @Info(
                title = "Minecraft Build Library API",
                version = "v0.1.0", // Match your application version if desired
                description = "API for managing and sharing Minecraft build "
                        + "schematics and metadata.",
                license = @License(name = "Apache 2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0.html")
        )
)

public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}