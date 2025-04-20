package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.NoSuchElementException;
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
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;

@RestController
@RequestMapping(StringConstants.THEMES_ENDPOINT)
@Validated
@Tag(name = "Themes", description = "API for managing build themes")
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @Operation(summary = "Create a new theme", description = "Creates a new theme resource.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Theme created "
            + "successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Theme.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (e.g., "
            + "blank name, "
                    + "duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                                    .class})))
    })
    @PostMapping
    public ResponseEntity<Theme> createTheme(
            @Parameter(description = "Name of the new theme", required = true, example = "Medieval")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String name) {
        Theme theme = themeService.createTheme(name);
        return new ResponseEntity<>(theme, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all themes", description = "Retrieves a list of all themes with "
            + "their related builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully "
            + "retrieved themes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto[].class)))
    })
    @GetMapping
    public ResponseEntity<List<ThemeDto>> getAllThemes() {
        List<ThemeDto> themes = themeService.findAllThemeDtos();
        return ResponseEntity.ok(themes);
    }

    @Operation(summary = "Get theme by identifier", description = "Retrieves a specific theme by "
            + "its ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully "
            + "retrieved theme",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto.class))), @ApiResponse(
                                    responseCode = "404", description = "Theme not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<ThemeDto> getThemeByIdentifier(
            @Parameter(description = "ID or exact name of the theme", required = true,
                    example = "2 or Medieval")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        ThemeDto themeDto = findThemeDtoByIdentifier(identifier);
        return ResponseEntity.ok(themeDto);
    }

    @Operation(summary = "Update a theme's name", description = "Updates the name of an existing "
            + "theme identified by ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Theme updated "
            + "successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Theme.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (e.g., "
            + "blank name, duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                                    .class}))), @ApiResponse(responseCode = "404",
            description = "Theme not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{identifier}")
    public ResponseEntity<Theme> updateTheme(
            @Parameter(description = "ID or exact name of the theme to update", required = true,
                    example = "2 or Medieval")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the theme", required = true,
                    example = "Fantasy")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String newName) {
        Theme theme = themeService.findThemes(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.THEME, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Theme updatedTheme = themeService.updateTheme(theme.getId(), newName);
        return ResponseEntity.ok(updatedTheme);
    }

    @Operation(summary = "Delete a theme", description = "Deletes a theme by ID or exact name. "
            + "Fails if the theme is associated with any builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204", description = "Theme deleted "
            + "successfully",
                    content = @Content), @ApiResponse(responseCode = "404",
            description = "Theme not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "409", description = "Theme is associated with "
            + "builds "
                    + "and cannot be deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteTheme(
            @Parameter(description = "ID or exact name of the theme to delete",
                    required = true, example = "2 or Medieval")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            themeService.deleteTheme(themeId);
        } catch (NumberFormatException e) {
            themeService.deleteThemeByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find themes by name query", description = "Finds themes using a "
            + "case-insensitive fuzzy name match (via SIMILARITY).")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully found "
            + "themes (list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto[].class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid query parameter "
            + "provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<ThemeDto>> getThemesByQuery(
            @Parameter(description = "Fuzzy name to search for themes.", example = "Med")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name
            /* @RequestParam Map<String, String> allParams - Reverted */) {

        List<ThemeDto> themes = themeService.findThemeDtos(name);
        return ResponseEntity.ok(themes);
    }

    // Helper method - no Swagger needed
    private ThemeDto findThemeDtoByIdentifier(String identifier) {
        try {
            Long themeId = Long.valueOf(identifier);
            return themeService.findThemeDtoById(themeId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.THEME, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<ThemeDto> themes = themeService.findThemeDtos(identifier);
            return themes.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.THEME, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}