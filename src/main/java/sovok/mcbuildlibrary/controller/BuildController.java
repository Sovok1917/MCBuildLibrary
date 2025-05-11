package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
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
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.AuthorService;
import sovok.mcbuildlibrary.service.BuildService;
import sovok.mcbuildlibrary.service.ColorService;
import sovok.mcbuildlibrary.service.ThemeService;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;


/**
 * Controller for managing Minecraft builds, including their metadata,
 * schematic files, and screenshots.
 * Provides endpoints for creating, retrieving, updating, and deleting builds.
 */
@RestController
@RequestMapping(StringConstants.BUILDS_ENDPOINT)
@Validated
@Tag(name = StringConstants.BUILDS, description = "API for managing Minecraft builds "
        + "(including schematics and "
        + "screenshots)")
public class BuildController {
    
    private final BuildService buildService;
    private final AuthorService authorService;
    private final ThemeService themeService;
    private final ColorService colorService;
    
    private static final int MAX_SCREENSHOTS = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    
    /**
     * Constructs the BuildController with necessary services.
     *
     * @param buildService  Service for build operations.
     * @param authorService Service for author operations.
     * @param themeService  Service for theme operations.
     * @param colorService  Service for color operations.
     */
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
                .map(authorService::findOrCreate)
                .collect(Collectors.toSet());
        Set<Theme> themes = themeNames.stream()
                .map(themeService::findOrCreate)
                .collect(Collectors.toSet());
        Set<Color> colors = colorNames.stream()
                .map(colorService::findOrCreate)
                .collect(Collectors.toSet());
        
        byte[] schemBytes = (schemFile != null && !schemFile.isEmpty()) ? schemFile.getBytes()
                : null;
        if (schemBytes == null && schemFile != null && !schemFile.isEmpty()) {
            throw new IOException("Failed to read bytes from schematic file: " + schemFile
                    .getOriginalFilename());
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
    
    
    /**
     * Creates a new build. Authors are taken directly from the request.
     *
     * @param name          The unique name for the build.
     * @param authorNames   List of author names.
     * @param themeNames    List of theme names.
     * @param description   Optional description.
     * @param colorNames    List of color names.
     * @param screenshots   Optional list of screenshot URLs.
     * @param schemFile     The .schem file.
     * @return A ResponseEntity containing the created Build and HTTP status 201.
     * @throws IOException if there's an error reading the schematic file.
     */
    @Operation(summary = "Create a new build", description = "Uploads a new build with metadata "
            + "and a schematic file. Authors are specified in the request.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201",
            description = "Build created successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Build.class))), @ApiResponse(
            responseCode = "400", description = "Invalid "
            + "input (missing fields, "
            + "validation errors, duplicate name, empty file)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail.class})))
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> createBuild(
            @Parameter(description = "Unique name for the build", required = true, example
                    = "MyAwesomeCastle")
            @RequestParam(StringConstants.NAME_REQ_PARAM) @NotBlank
            @Size(min = 3, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name,
            
            @Parameter(description = "List of author names associated with the build",
                    required = true, example = "BuilderBob")
            @RequestParam(StringConstants.AUTHORS_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> authorNames,
            
            @Parameter(description = "List of theme names associated with the build",
                    required = true, example = "Medieval")
            @RequestParam(StringConstants.THEMES_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> themeNames,
            
            @Parameter(description = "Optional description of the build",
                    example = "A large stone castle with a moat.")
            @RequestParam(value = StringConstants.DESCRIPTION_REQ_PARAM, required = false)
            @Size(max = MAX_DESCRIPTION_LENGTH) String description,
            
            @Parameter(description = "List of color names associated with the build",
                    required = true, example = "Stone")
            @RequestParam(StringConstants.COLORS_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> colorNames,
            
            @Parameter(description = "List of URLs or identifiers for screenshots (max 10)",
                    example = "https://example.com/screenshot1.png")
            @RequestParam(value = StringConstants.SCREENSHOTS_REQ_PARAM, required = false)
            @Size(max = MAX_SCREENSHOTS) List<@NotBlank String> screenshots,
            
            @Parameter(description = "The .schem file for the build", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam(StringConstants.SCHEM_FILE_REQ_PARAM) @NotNull MultipartFile schemFile
    ) throws IOException {
        
        if (schemFile.isEmpty()) {
            throw new IllegalArgumentException(StringConstants.FILE_NOT_EMPTY);
        }
        
        // Author names are taken directly from the request.
        // Ensure uniqueness if desired, but no automatic addition of principal.
        List<String> finalAuthorNames = new ArrayList<>(new HashSet<>(authorNames));
        
        Build buildToCreate = createBuildFromParams(name, finalAuthorNames, themeNames, description,
                colorNames, screenshots, schemFile);
        Build createdBuild = buildService.createBuild(buildToCreate);
        return new ResponseEntity<>(createdBuild, HttpStatus.CREATED);
    }
    
    /**
     * Retrieves a specific build by its ID or exact name.
     *
     * @param identifier The ID or exact name of the build.
     * @return ResponseEntity containing the found Build or 404 if not found.
     */
    @Operation(summary = "Get build by identifier", description = "Retrieves a specific build by "
            + "its ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved build",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Build.class))), @ApiResponse(
            responseCode = "404", description = "Build not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<Build> getBuildByIdentifier(
            @Parameter(description = "ID or exact name of the build", required = true,
                    example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        Build build = findBuildByIdentifierInternal(identifier);
        return ResponseEntity.ok(build);
    }
    
    /**
     * Retrieves a list of all builds (metadata only).
     *
     * @return ResponseEntity containing a list of all Builds.
     */
    @Operation(summary = "Get all builds", description = "Retrieves a list of all builds "
            + "(metadata only).")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved builds",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Build[].class)))
    })
    @GetMapping
    public ResponseEntity<List<Build>> getAllBuilds() {
        List<Build> builds = buildService.findAll();
        return ResponseEntity.ok(builds);
    }
    
    /**
     * Finds builds using fuzzy matching on author, name, theme, and color.
     *
     * @param author Optional fuzzy author name filter.
     * @param name   Optional fuzzy build name filter.
     * @param theme  Optional fuzzy theme name filter.
     * @param color  Optional fuzzy color name filter.
     * @return ResponseEntity containing a list of filtered Builds.
     */
    @Operation(summary = "Find builds by query parameters", description = "Finds builds using "
            + "fuzzy matching on author, name, theme, and color.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description
            = "Successfully found builds (list "
            + "might be empty)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Build[].class))), @ApiResponse(
            responseCode = "400", description = "Invalid query "
            + "parameter provided",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<Build>> getBuildsByQuery(
            @Parameter(description = "Fuzzy author name filter",
                    example = "BldrBob")
            @RequestParam(value = StringConstants.AUTHOR_QUERY_PARAM, required = false)
            String author,
            @Parameter(description = "Fuzzy build name filter",
                    example = "Castle")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false)
            String name,
            @Parameter(description = "Fuzzy theme name filter",
                    example = "Mediev")
            @RequestParam(value = StringConstants.THEME_QUERY_PARAM, required = false)
            String theme,
            @Parameter(description = "Fuzzy color name filter",
                    example = "Stone")
            @RequestParam(value = StringConstants.COLOR_QUERY_PARAM, required = false)
            String color) {
        
        List<Build> filteredBuilds = buildService.filterBuilds(author, name, theme, color);
        return ResponseEntity.ok(filteredBuilds);
    }
    
    /**
     * Retrieves a list of all screenshot URLs/identifiers for a specific build.
     *
     * @param identifier The ID or exact name of the build.
     * @return ResponseEntity containing a list of screenshot URLs.
     */
    @Operation(summary = "Get all screenshot links for a build", description = "Retrieves a list "
            + "of all screenshot URLs/identifiers associated with a specific build.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved screenshot "
                    + "list (may be empty)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = String[].class))), @ApiResponse(
            responseCode = "404", description = "Build not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}/screenshots")
    public ResponseEntity<List<String>> getScreenshots(
            @Parameter(description = "ID or exact name of the build", required = true,
                    example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        Build build = findBuildByIdentifierInternal(identifier);
        List<String> screenshots = build.getScreenshots();
        return ResponseEntity.ok(screenshots != null ? screenshots : Collections.emptyList());
    }
    
    /**
     * Updates an existing build.
     *
     * @param identifier    The ID or exact name of the build to update.
     * @param name          The new unique name for the build.
     * @param authorNames   The new list of author names.
     * @param themeNames    The new list of theme names.
     * @param description   The new optional description.
     * @param colorNames    The new list of color names.
     * @param screenshots   The new optional list of screenshot URLs.
     * @param schemFile     Optional new .schem file to replace the existing one.
     * @return ResponseEntity containing the updated Build or relevant error status.
     * @throws IOException if there's an error reading the new schematic file.
     */
    @Operation(summary = "Update an existing build", description = "Updates the metadata and "
            + "optionally the schematic file for an existing build. (Admin only)")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Build updated successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = Build.class))), @ApiResponse(
            responseCode = "400", description = "Invalid input "
            + "(missing fields, "
            + "validation errors, duplicate name, empty file)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                            .class}))), @ApiResponse(responseCode = "404",
            description = "Build not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{identifier}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Build> updateBuild(
            @Parameter(description = "ID or exact name of the build to update", required = true,
                    example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            
            @Parameter(description = "Unique name for the build", required = true, example
                    = "MyUpdatedCastle")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank
            @Size(min = 3, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name,
            @Parameter(description = "List of author names", required = true, example
                    = "BuilderBob,ArchitectAnna")
            @RequestParam(StringConstants.AUTHORS_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> authorNames,
            @Parameter(description = "List of theme names", required = true, example
                    = "Medieval,Fantasy")
            @RequestParam(StringConstants.THEMES_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> themeNames,
            @Parameter(description = "Optional description", example = "An updated stone castle.")
            @RequestParam(value = StringConstants.DESCRIPTION_REQ_PARAM, required = false)
            @Size(max = MAX_DESCRIPTION_LENGTH)
            String description,
            @Parameter(description = "List of color names", required = true, example = "Stone,Gray")
            @RequestParam(StringConstants.COLORS_REQ_PARAM) @NotEmpty @Size(min = 1)
            List<@NotBlank String> colorNames,
            @Parameter(description = "List of screenshot URLs/identifiers (max 10)", example
                    = "https://example.com/new_screenshot.png")
            @RequestParam(value = StringConstants.SCREENSHOTS_REQ_PARAM, required = false)
            @Size(max = MAX_SCREENSHOTS) List<@NotBlank String> screenshots,
            @Parameter(description = "Optional: New .schem file to replace the existing one",
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam(value = StringConstants.SCHEM_FILE_REQ_PARAM, required = false)
            MultipartFile schemFile
    ) throws IOException {
        
        Build existingBuild = findBuildByIdentifierInternal(identifier);
        
        if (schemFile != null && schemFile.isEmpty()) {
            throw new IllegalArgumentException(StringConstants.FILE_NOT_EMPTY);
        }
        
        List<String> finalAuthorNames = new ArrayList<>(new HashSet<>(authorNames));
        
        Build updatedBuildData = createBuildFromParams(name, finalAuthorNames, themeNames,
                description,
                colorNames, screenshots, schemFile);
        Build result = buildService.updateBuild(existingBuild.getId(), updatedBuildData);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Deletes a build by its ID or exact name.
     *
     * @param identifier The ID or exact name of the build to delete.
     * @return ResponseEntity with status 204 if successful, or 404 if not found.
     */
    @Operation(summary = "Delete a build", description = "Deletes a build by its ID or "
            + "exact name, including its schematic and associations. (Admin only)")
    @ApiResponses(value = {@ApiResponse(responseCode = "204",
            description = "Build deleted successfully",
            content = @Content), @ApiResponse(responseCode = "404",
            description = "Build not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteBuild(
            @Parameter(description = "ID or exact name of the build to delete", required = true,
                    example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        Build build = findBuildByIdentifierInternal(identifier);
        buildService.deleteBuild(build.getId());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Downloads the .schem file for a specific build.
     *
     * @param identifier The ID or exact name of the build.
     * @return ResponseEntity containing the schematic file bytes or 404 if not found.
     */
    @Operation(summary = "Download schematic file", description = "Downloads the .schem file "
            + "for a specific build.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Schematic "
            + "file download initiated",
            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)), @ApiResponse(
            responseCode = "404", description = "Build or schematic file not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}/schem")
    public ResponseEntity<byte[]> getSchemFile(
            @Parameter(description = "ID or exact name of the build", required = true,
                    example = "10 or MyAwesomeCastle")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        Build build = findBuildByIdentifierInternal(identifier);
        byte[] schemFileBytes = buildService.getSchemFile(build.getId())
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.SCHEM_FILE_FOR_BUILD_NOT_FOUND, identifier)));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String filename = build.getName().replaceAll("[^a-zA-Z0-9-_.]+", "_")
                .replaceAll("[\\\\/]", "_") + ".schem";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(schemFileBytes.length);
        return new ResponseEntity<>(schemFileBytes, headers, HttpStatus.OK);
    }
    
    private Build findBuildByIdentifierInternal(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            return buildService.findBuildById(buildId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            return buildService.findByName(identifier)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.BUILD, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}