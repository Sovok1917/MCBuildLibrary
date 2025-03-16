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
import sovok.mcbuildlibrary.exception.NoBuildsFoundException;
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
                .orElseThrow(() -> new NoBuildsFoundException("No build found with ID: " + id));
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        if (builds == null || builds.isEmpty()) {
            throw new NoBuildsFoundException("No builds are currently available.");
        }
        return ResponseEntity.ok(builds);
    }

    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String theme,
            @RequestParam(value = "color", required = false) List<String> colors,
            @RequestParam MultiValueMap<String, String> allParams) {

        // Validate query parameters
        Set<String> allowedParams = Set.of("author", "name", "theme", "color");
        for (String param : allParams.keySet()) {
            if (!allowedParams.contains(param)) {
                throw new InvalidQueryParameterException(param);
            }
        }

        // Filter builds and handle empty results
        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, colors);
        if (filteredBuilds == null || filteredBuilds.isEmpty()) {
            throw new NoBuildsFoundException();
        }

        return ResponseEntity.ok(filteredBuilds);
    }
}
