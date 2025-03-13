package sovok.mcbuildlibrary.controller;

import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.service.BuildService;

@RestController
@RequestMapping("/builds")
public class BuildController {

    private final BuildService buildService;

    public BuildController(BuildService buildService) {
        this.buildService = buildService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Build> getBuildById(@PathVariable String id) {
        return buildService.findBuildById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        return ResponseEntity.ok(builds);
    }

    /**
     * Returns builds that match the provided query parameters.
     * All parameters are optional, and they can be combined.
     * Throws a 400 error if any unknown parameter is provided.
     * Example query:
     *   /builds/query?author=John&name=Castle&theme=Medieval&color=red&color=blue
     */
    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String theme,
            @RequestParam(value = "color", required = false) List<String> colors,
            @RequestParam MultiValueMap<String, String> allParams) {

        // Define the allowed query parameter keys.
        Set<String> allowedParams = Set.of("author", "name", "theme", "color");
        for (String param : allParams.keySet()) {
            if (!allowedParams.contains(param)) {
                throw new InvalidQueryParameterException(param);
            }
        }

        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, colors);
        if (filteredBuilds == null || filteredBuilds.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(filteredBuilds);
    }

    // Returns a specific screenshot (by index) for the Build with the given id.
    @GetMapping("/{id}/screenshot")
    public ResponseEntity<String> getScreenshot(@PathVariable String id,
                                                @RequestParam int index) {
        return buildService.getScreenshot(id, index)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
