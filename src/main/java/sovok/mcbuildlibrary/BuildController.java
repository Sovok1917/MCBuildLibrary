package sovok.mcbuildlibrary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BuildController {

    @GetMapping("/builds/{id}")
    public Build getBuildById(@PathVariable String id) {
        // Returning a new Build record instance
        return new Build(id, "Sample Build");
    }

    @GetMapping("/builds")
    public Build getBuildByName(@RequestParam String name) {
        // For demonstration purposes, return a Build with a sample ID
        return new Build("sample-id", name);
    }

}
