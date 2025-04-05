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
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.exception.StringConstants;
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

    // Define constants for parameter names
    private static final String IDENTIFIER_PATH_VAR = "identifier";
    private static final String NAME_REQ_PARAM = "name";
    private static final String AUTHORS_REQ_PARAM = "authors";
    private static final String THEMES_REQ_PARAM = "themes";
    private static final String DESCRIPTION_REQ_PARAM = "description";
    private static final String COLORS_REQ_PARAM = "colors"; // Keep this for create/update where
    // multiple are expected
    private static final String SCREENSHOTS_REQ_PARAM = "screenshots";
    private static final String SCHEM_FILE_REQ_PARAM = "schemFile";
    private static final String AUTHOR_QUERY_PARAM = "author";
    private static final String THEME_QUERY_PARAM = "theme";
    private static final String COLOR_QUERY_PARAM = "color"; // Use this for the single color query
    // param
    private static final String INDEX_QUERY_PARAM = "index";


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
                                        // Still accepts list here
                                        List<String> screenshots,
                                        MultipartFile schemFile) throws IOException {
        Set<Author> authors = authorNames.stream()
                .map(authorService::findOrCreateAuthor)
                .collect(Collectors.toSet());
        Set<Theme> themes = themeNames.stream()
                .map(themeService::findOrCreateTheme)
                .collect(Collectors.toSet());
        Set<Color> colors = colorNames.stream() // Processes list for creation
                .map(colorService::findOrCreateColor)
                .collect(Collectors.toSet());

        byte[] schemBytes = (schemFile != null && !schemFile.isEmpty()) ? schemFile.getBytes()
                : null;

        return Build.builder()
                .name(name)
                .authors(authors)
                .themes(themes)
                .description(description)
                .colors(colors) // Assigns the Set<Color>
                .screenshots(screenshots != null ? screenshots : List.of())
                .schemFile(schemBytes)
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> createBuild(
            @RequestParam(NAME_REQ_PARAM) String name,
            @RequestParam(AUTHORS_REQ_PARAM) List<String> authorNames,
            @RequestParam(THEMES_REQ_PARAM) List<String> themeNames,
            @RequestParam(value = DESCRIPTION_REQ_PARAM, required = false) String description,
            @RequestParam(COLORS_REQ_PARAM) List<String> colorNames, // Post accepts multiple colors
            @RequestParam(value = SCREENSHOTS_REQ_PARAM, required = false) List<String> screenshots,
            @RequestParam(SCHEM_FILE_REQ_PARAM) MultipartFile schemFile) throws IOException {

        Build buildToCreate = createBuildFromParams(name, authorNames, themeNames, description,
                colorNames,
                screenshots, schemFile);
        Build createdBuild = buildService.createBuild(buildToCreate);
        return new ResponseEntity<>(createdBuild, HttpStatus.CREATED);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Build> getBuildByIdentifier(@PathVariable(IDENTIFIER_PATH_VAR)
                                                          String identifier) {
        Build build = findBuildByIdentifier(identifier);
        return ResponseEntity.ok(build);
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        if (builds.isEmpty()) {
            throw new ResourceNotFoundException(String.format(StringConstants.NO_ENTITIES_AVAILABLE,
                    "builds"));
        }
        return ResponseEntity.ok(builds);
    }

    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam(required = false, value = AUTHOR_QUERY_PARAM) String author,
            @RequestParam(required = false, value = NAME_REQ_PARAM) String name,
            @RequestParam(required = false, value = THEME_QUERY_PARAM) String theme,
            // Updated: Accept single String for 'color' query parameter
            @RequestParam(value = COLOR_QUERY_PARAM, required = false) String color) {
        // Changed from List<String> to String

        // Call service method with the single color string
        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, color);
        if (filteredBuilds.isEmpty()) {
            throw new ResourceNotFoundException(String.format(StringConstants.QUERY_NO_RESULTS,
                    "builds", "provided criteria"));
        }
        return ResponseEntity.ok(filteredBuilds);
    }

    @GetMapping("/{identifier}/screenshot")
    public ResponseEntity<String> getScreenshot(@PathVariable(IDENTIFIER_PATH_VAR)
                                                    String identifier,
                                                @RequestParam(INDEX_QUERY_PARAM) int index) {
        Build build = findBuildByIdentifier(identifier);
        return buildService.getScreenshot(build.getId(), index)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format("Screenshot index %d for %s %s '%s' %s", index,
                                StringConstants.BUILD,
                                StringConstants.WITH_ID, build.getId(),
                                StringConstants.NOT_FOUND_MESSAGE)));
    }

    @PutMapping(value = "/{identifier}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> updateBuild(
            @PathVariable(IDENTIFIER_PATH_VAR) String identifier,
            @RequestParam(NAME_REQ_PARAM) String name,
            @RequestParam(AUTHORS_REQ_PARAM) List<String> authorNames,
            @RequestParam(THEMES_REQ_PARAM) List<String> themeNames,
            @RequestParam(value = DESCRIPTION_REQ_PARAM, required = false) String description,
            @RequestParam(COLORS_REQ_PARAM) List<String> colorNames,
            @RequestParam(value = SCREENSHOTS_REQ_PARAM, required = false) List<String> screenshots,
            @RequestParam(value = SCHEM_FILE_REQ_PARAM, required = false) MultipartFile schemFile)
            throws IOException {

        Build existingBuild = findBuildByIdentifier(identifier);
        Build updatedBuildData = createBuildFromParams(name, authorNames, themeNames, description,
                colorNames, screenshots, schemFile);
        Build result = buildService.updateBuild(existingBuild.getId(), updatedBuildData);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteBuild(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        Build build = findBuildByIdentifier(identifier);
        buildService.deleteBuild(build.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/schem")
    public ResponseEntity<byte[]> getSchemFile(@PathVariable(IDENTIFIER_PATH_VAR)
                                                   String identifier) {
        Build build = findBuildByIdentifier(identifier);
        byte[] schemFile = buildService.getSchemFile(build.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(StringConstants.SCHEM_FILE_FOR_BUILD_NOT_FOUND, identifier)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = build.getName().replaceAll("[^a-zA-Z0-9-_.]", "_")
                + ".schem";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(schemFile.length);
        return new ResponseEntity<>(schemFile, headers, HttpStatus.OK);
    }

    private Build findBuildByIdentifier(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            return buildService.findBuildById(buildId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            return buildService.findByName(identifier)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}