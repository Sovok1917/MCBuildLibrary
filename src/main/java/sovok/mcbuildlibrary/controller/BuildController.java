package sovok.mcbuildlibrary.controller;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.AuthorService;
import sovok.mcbuildlibrary.service.BuildService;
import sovok.mcbuildlibrary.service.ColorService;
import sovok.mcbuildlibrary.service.ThemeService;

@RestController
@RequestMapping("/builds")
public class BuildController {

    private final BuildService buildService;
    private final AuthorService authorService;
    private final ThemeService themeService;
    private final ColorService colorService;

    public BuildController(BuildService buildService, AuthorService authorService,
                           ThemeService themeService, ColorService colorService) {
        this.buildService = buildService;
        this.authorService = authorService;
        this.themeService = themeService;
        this.colorService = colorService;
    }

    private Build createBuildFromParams(String name, List<String> authorNames,
                                        List<String> themeNames,
                                        String description, List<String> colorNames,
                                        List<String> screenshots,
                                        MultipartFile schemFile) throws IOException {
        Set<Author> authors = authorNames.stream()
                .map(authorService::findOrCreateAuthor)
                .collect(Collectors.toSet());
        Set<Theme> themes = themeNames.stream()
                .map(themeService::findOrCreateTheme)
                .collect(Collectors.toSet());
        Set<Color> colors = colorNames.stream()
                .map(colorService::findOrCreateColor)
                .collect(Collectors.toSet());
        return Build.builder()
                .name(name)
                .authors(authors)
                .themes(themes)
                .description(description)
                .colors(colors)
                .screenshots(screenshots)
                .schemFile(schemFile.getBytes())
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> createBuild(
            @RequestParam("name") String name,
            @RequestParam("authors") List<String> authorNames,
            @RequestParam("themes") List<String> themeNames,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("colors") List<String> colorNames,
            @RequestParam(value = "screenshots", required = false) List<String> screenshots,
            @RequestParam("schemFile") MultipartFile schemFile) throws IOException {
        Build build = createBuildFromParams(name, authorNames, themeNames, description, colorNames,
                screenshots, schemFile);
        Build createdBuild = buildService.createBuild(build);
        return new ResponseEntity<>(createdBuild, HttpStatus.CREATED);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Build> getBuildByIdentifier(@PathVariable String identifier) {
        Build build = findBuildByIdentifier(identifier);
        return ResponseEntity.ok(build);
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        if (builds == null || builds.isEmpty()) {
            throw new ResourceNotFoundException("No builds are currently available");
        }
        return ResponseEntity.ok(builds);
    }

    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String theme,
            @RequestParam(value = "color", required = false) List<String> colors) {
        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, colors);
        if (filteredBuilds == null || filteredBuilds.isEmpty()) {
            throw new ResourceNotFoundException("No builds found matching the query");
        }
        return ResponseEntity.ok(filteredBuilds);
    }

    @GetMapping("/{identifier}/screenshot")
    public ResponseEntity<String> getScreenshot(@PathVariable String identifier,
                                                @RequestParam int index) {
        Build build = findBuildByIdentifier(identifier);
        return buildService.getScreenshot(build.getId(), index)
                .map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.GONE));
    }

    @PutMapping(value = "/{identifier}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> updateBuild(
            @PathVariable String identifier,
            @RequestParam("name") String name,
            @RequestParam("authors") List<String> authorNames,
            @RequestParam("themes") List<String> themeNames,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("colors") List<String> colorNames,
            @RequestParam(value = "screenshots", required = false) List<String> screenshots,
            @RequestParam("schemFile") MultipartFile schemFile) throws IOException {
        Build build = findBuildByIdentifier(identifier);
        Build updatedBuild = createBuildFromParams(name, authorNames, themeNames, description,
                colorNames, screenshots, schemFile);
        Build result = buildService.updateBuild(build.getId(), updatedBuild);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteBuild(@PathVariable String identifier) {
        Build build = findBuildByIdentifier(identifier);
        buildService.deleteBuild(build.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/schem")
    public ResponseEntity<byte[]> getSchemFile(@PathVariable String identifier) {
        Build build = findBuildByIdentifier(identifier);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = build.getName().replaceAll("[^a-zA-Z0-9-_ ]", "")
                + ".schem";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(build.getSchemFile().length);
        return new ResponseEntity<>(build.getSchemFile(), headers, HttpStatus.OK);
    }

    private Build findBuildByIdentifier(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            return buildService.findBuildById(buildId)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.BUILD_WITH_ID
                            + " " + identifier + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            return buildService.findByName(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.BUILD_WITH_NAME
                            + " " + identifier + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        }
    }
}