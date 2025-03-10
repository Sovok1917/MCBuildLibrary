package sovok.mcbuildlibrary.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.model.Build;

@RestController
public class BuildController {

    @GetMapping("/builds/{id}")
    public Build getBuildById(@PathVariable String id) {
        // Constructing a Build object with sample data for all fields.
        return new Build(
                id,
                "Sample Build",
                "Sample Author",
                "Medieval",
                "This is a sample description for a medieval-themed build.",
                List.of("Red", "Green", "Blue"),
                List.of("https://example.com/screenshot1.jpg", "https://example.com/screenshot2.jpg"),
                "https://example.com/sample.schem"
        );
    }

    @GetMapping("/builds")
    public Build getBuildByName(@RequestParam String name) {
        // Constructing a Build object using the provided name and sample data.
        return new Build(
                "sample-id",   // Sample ID value
                name,
                "Sample Author",
                "Modern",
                "This is a demonstration build for retrieving by name.",
                List.of("Yellow", "Black"),
                List.of("https://example.com/screenshot.jpg"),
                "https://example.com/build.schem"
        );
    }
}
