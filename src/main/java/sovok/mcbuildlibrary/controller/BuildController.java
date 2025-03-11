package sovok.mcbuildlibrary.controller;

import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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
        Optional<Build> build = buildService.findBuildById(id);
        return build.map(ResponseEntity::ok)
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
     * If no matching builds are found, a 404 is returned.
     * Example query:
     *   /api/builds/query?author=John&name=Castle&theme=Medieval&colors=red&colors=blue
     */
    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String theme,
            @RequestParam(required = false) List<String> colors) {

        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, colors);
        if (filteredBuilds == null || filteredBuilds.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(filteredBuilds);
    }

    //Returns a specific screenshot (by index) for the Build with the given id.
    @GetMapping("/{id}/screenshot")
    public ResponseEntity<String> getScreenshot(@PathVariable String id,
                                                @RequestParam int index) {
        Optional<String> screenshot = buildService.getScreenshot(id, index);
        return screenshot.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}