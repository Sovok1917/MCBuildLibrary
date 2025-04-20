// file: src/main/java/sovok/mcbuildlibrary/controller/ColorController.java
package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;

import java.util.List;

@RestController
@RequestMapping(StringConstants.COLORS_ENDPOINT) // Define specific path here
@Tag(name = StringConstants.COLORS, description = "API for managing build colors") // Specific Tag
public class ColorController extends BaseNamedEntityController<Color, ColorDto, ColorService> {

    @Autowired
    public ColorController(ColorService colorService) {
        super(colorService);
    }

    @Override
    protected String getEntityTypeName() {
        return StringConstants.COLOR;
    }

    @Override
    protected String getEntityTypePluralName() {
        return StringConstants.COLORS;
    }

    @Override
    protected String getEntityNameExample() {
        return "DarkOak";
    }

    @Override
    protected String getEntityIdentifierExample() {
        return "5 or DarkOak";
    }


    // --- Optional Overrides for OpenAPI Schema Specificity ---

    @Override
    @PostMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Color created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Color.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (blank name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception.ValidationErrorResponse.class, org.springframework.http.ProblemDetail.class})))
    })
    public ResponseEntity<Color> createEntity(
            @Parameter(description = "Name of the new color", required = true, example = "DarkOak")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String name) {
        return super.createEntity(name);
    }

    @Override
    @GetMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved colors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto[].class)))
    })
    public ResponseEntity<List<ColorDto>> getAllEntities() {
        return super.getAllEntities();
    }

    @Override
    @GetMapping("/{identifier}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved color",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto.class))),
            @ApiResponse(responseCode = "404", description = "Color not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<ColorDto> getEntityByIdentifier(
            @Parameter(description = "ID or exact name of the color", required = true, example = "5 or DarkOak")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        return super.getEntityByIdentifier(identifier);
    }

    @Override
    @PutMapping("/{identifier}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Color updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Color.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (blank name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception.ValidationErrorResponse.class, org.springframework.http.ProblemDetail.class}))),
            @ApiResponse(responseCode = "404", description = "Color not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Color> updateEntity(
            @Parameter(description = "ID or exact name of the color to update", required = true, example = "5 or DarkOak")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the color", required = true, example = "SprucePlanks")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String newName) {
        return super.updateEntity(identifier, newName);
    }

    @Override
    @GetMapping("/query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found colors (list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto[].class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<List<ColorDto>> getEntitiesByQuery(
            @Parameter(description = "Fuzzy name to search for colors.", example = "Oak")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name) {
        return super.getEntitiesByQuery(name);
    }

    // Delete uses base implementation
}