// file: src/main/java/sovok/mcbuildlibrary/controller/BuildController.java
package sovok.mcbuildlibrary.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Import
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
// Removed import for ResourceNotFoundException
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
@Validated
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

        byte[] schemBytes = (schemFile != null && !schemFile.isEmpty()) ? schemFile.getBytes() : null;
        if (schemBytes == null && schemFile != null) {
            throw new IOException("Failed to read bytes from schematic file.");
        }

        return Build.builder()
                .name(name)
                .authors(authors)
                .themes(themes)
                .description(description)
                .colors(colors)
                .screenshots(screenshots != null ? screenshots : List.of())
                .schemFile(schemBytes)
                .build();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> createBuild(
            // ... validated request parameters ...
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 3, message = StringConstants.NAME_SIZE)
            String name,

            @RequestParam(StringConstants.AUTHORS_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one author required")
            List<@NotBlank(message = "Author name cannot be blank") String> authorNames,

            @RequestParam(StringConstants.THEMES_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one theme required")
            List<@NotBlank(message = "Theme name cannot be blank") String> themeNames,

            @RequestParam(value = StringConstants.DESCRIPTION_REQ_PARAM, required = false)
            @Size(message = "Description cannot exceed {max} characters")
            String description,

            @RequestParam(StringConstants.COLORS_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one color required")
            List<@NotBlank(message = "Color name cannot be blank") String> colorNames,

            @RequestParam(value = StringConstants.SCREENSHOTS_REQ_PARAM, required = false)
            @Size(max = 10, message = "Maximum of 10 screenshots allowed")
            List<@NotBlank(message = "Screenshot URL/identifier cannot be blank") String> screenshots,

            @RequestParam(StringConstants.SCHEM_FILE_REQ_PARAM)
            @NotNull(message = StringConstants.FILE_NOT_EMPTY)
            MultipartFile schemFile) throws IOException {

        if (schemFile.isEmpty()) {
            throw new IllegalArgumentException(StringConstants.FILE_NOT_EMPTY);
        }
        // Service handles duplicate name check (IllegalArgumentException)
        Build buildToCreate = createBuildFromParams(name, authorNames, themeNames, description,
                colorNames, screenshots, schemFile);
        Build createdBuild = buildService.createBuild(buildToCreate);
        return new ResponseEntity<>(createdBuild, HttpStatus.CREATED);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Build> getBuildByIdentifier(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException
        Build build = findBuildByIdentifier(identifier);
        return ResponseEntity.ok(build);
    }

    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        return ResponseEntity.ok(builds);
    }

    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQueryParams(
            @RequestParam Map<String, String> allParams) {

        validateQueryParameters(allParams, StringConstants.ALLOWED_BUILD_QUERY_PARAMS);

        String author = allParams.get(StringConstants.AUTHOR_QUERY_PARAM);
        String name = allParams.get(StringConstants.NAME_REQ_PARAM);
        String theme = allParams.get(StringConstants.THEME_QUERY_PARAM);
        String color = allParams.get(StringConstants.COLOR_QUERY_PARAM);

        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, color);
        return ResponseEntity.ok(filteredBuilds); // Return OK even if empty
    }

    @GetMapping("/{identifier}/screenshots")
    public ResponseEntity<List<String>> getScreenshots(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException if build not found
        Build build = findBuildByIdentifier(identifier);
        List<String> screenshots = build.getScreenshots();
        return ResponseEntity.ok(screenshots != null ? screenshots : Collections.emptyList());
    }

    @PutMapping(value = "/{identifier}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> updateBuild(
            // ... validated path variable and request parameters ...
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,

            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 3, message = StringConstants.NAME_SIZE)
            String name,

            @RequestParam(StringConstants.AUTHORS_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one author required")
            List<@NotBlank(message = "Author name cannot be blank") String> authorNames,

            @RequestParam(StringConstants.THEMES_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one theme required")
            List<@NotBlank(message = "Theme name cannot be blank") String> themeNames,

            @RequestParam(value = StringConstants.DESCRIPTION_REQ_PARAM, required = false)
            @Size(message = "Description cannot exceed {max} characters")
            String description,

            @RequestParam(StringConstants.COLORS_REQ_PARAM)
            @NotEmpty(message = StringConstants.LIST_NOT_EMPTY)
            @Size(min = 1, message = "At least one color required")
            List<@NotBlank(message = "Color name cannot be blank") String> colorNames,

            @RequestParam(value = StringConstants.SCREENSHOTS_REQ_PARAM, required = false)
            @Size(max = 10, message = "Maximum of 10 screenshots allowed")
            List<@NotBlank(message = "Screenshot URL/identifier cannot be blank") String> screenshots,

            @RequestParam(value = StringConstants.SCHEM_FILE_REQ_PARAM, required = false)
            MultipartFile schemFile)
            throws IOException {

        // Helper throws NoSuchElementException if build not found
        Build existingBuild = findBuildByIdentifier(identifier);

        if (schemFile != null && schemFile.isEmpty()) {
            throw new IllegalArgumentException(StringConstants.FILE_NOT_EMPTY);
        }
        // Service handles duplicate name check (IllegalArgumentException)
        Build updatedBuildData = createBuildFromParams(name, authorNames, themeNames, description,
                colorNames, screenshots, schemFile);
        Build result = buildService.updateBuild(existingBuild.getId(), updatedBuildData);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteBuild(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException if build not found
        Build build = findBuildByIdentifier(identifier);
        buildService.deleteBuild(build.getId()); // Service might throw internally, but deleteById is void
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/schem")
    public ResponseEntity<byte[]> getSchemFile(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper throws NoSuchElementException if build not found
        Build build = findBuildByIdentifier(identifier);
        // Service throws NoSuchElementException if schem file is missing/empty
        byte[] schemFileBytes = buildService.getSchemFile(build.getId())
                .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                        String.format(StringConstants.SCHEM_FILE_FOR_BUILD_NOT_FOUND, identifier)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = build.getName().replaceAll("[^a-zA-Z0-9-_.]+", "_").replaceAll("[\\\\/]", "_") + ".schem";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(schemFileBytes.length);
        return new ResponseEntity<>(schemFileBytes, headers, HttpStatus.OK);
    }

    private void validateQueryParameters(Map<String, String> receivedParams, Set<String> allowedParams) {
        for (String paramName : receivedParams.keySet()) {
            if (!allowedParams.contains(paramName)) {
                // Throw IllegalArgumentException for invalid query parameter
                throw new IllegalArgumentException(
                        String.format(StringConstants.INVALID_QUERY_PARAMETER_DETECTED,
                                paramName,
                                String.join(", ", allowedParams.stream().sorted().toList()))
                );
            }
        }
    }

    private Build findBuildByIdentifier(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            // Service findBuildById returns Optional, throw NoSuchElementException if empty
            return buildService.findBuildById(buildId)
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            // Service findByName returns Optional, throw NoSuchElementException if empty
            return buildService.findByName(identifier)
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}