package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.service.ThemeService;


@RestController
@RequestMapping(StringConstants.THEMES_ENDPOINT) // Define specific path here
@Tag(name = StringConstants.THEMES, description = "API for managing build themes") // Specific Tag
public class ThemeController extends BaseNamedEntityController<Theme, ThemeDto, ThemeService> {

    @Autowired
    public ThemeController(ThemeService themeService) {
        super(themeService);
    }

    @Override
    protected String getEntityTypeName() {
        return StringConstants.THEME;
    }

    @Override
    protected String getEntityTypePluralName() {
        return StringConstants.THEMES;
    }

    @Override
    protected String getEntityNameExample() {
        return "Medieval";
    }

    @Override
    protected String getEntityIdentifierExample() {
        return "2 or Medieval";
    }


    @Override
    @PostMapping
    @ApiResponses(value = {@ApiResponse(responseCode = "201",
            description = "Theme created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Theme.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (blank "
            + "name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception
                                    .ValidationErrorResponse.class, org.springframework.http
                                    .ProblemDetail.class})))
    })
    public ResponseEntity<Theme> createEntity(
            @Parameter(description = "Name of the new theme", required = true, example = "Medieval")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String name) {
        return super.createEntity(name);
    }

    @Override
    @GetMapping
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved themes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto[].class)))
    })
    public ResponseEntity<List<ThemeDto>> getAllEntities() {
        return super.getAllEntities();
    }

    @Override
    @GetMapping("/{identifier}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description =
            "Successfully retrieved theme",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto.class))), @ApiResponse(
                                    responseCode = "404", description = "Theme not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http
                                    .ProblemDetail.class)))
    })
    public ResponseEntity<ThemeDto> getEntityByIdentifier(
            @Parameter(description = "ID or exact name of the theme", required = true,
                    example = "2 or Medieval")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        return super.getEntityByIdentifier(identifier);
    }

    @Override
    @PutMapping("/{identifier}")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Theme updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Theme.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (blank "
            + "name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception
                                    .ValidationErrorResponse.class, org.springframework.http
                                    .ProblemDetail.class}))), @ApiResponse(responseCode = "404",
            description = "Theme not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http
                                    .ProblemDetail.class)))
    })
    public ResponseEntity<Theme> updateEntity(
            @Parameter(description = "ID or exact name of the theme to update", required = true,
                    example = "2 or Medieval")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the theme", required = true,
                    example = "Fantasy")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String newName) {
        return super.updateEntity(identifier, newName);
    }

    @Override
    @GetMapping("/query")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description
            = "Successfully found themes "
                    + "(list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ThemeDto[].class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid query "
            + "parameter provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http
                                    .ProblemDetail.class)))
    })
    public ResponseEntity<List<ThemeDto>> getEntitiesByQuery(
            @Parameter(description = "Fuzzy name to search for themes.", example = "Med")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name) {
        return super.getEntitiesByQuery(name);
    }
}