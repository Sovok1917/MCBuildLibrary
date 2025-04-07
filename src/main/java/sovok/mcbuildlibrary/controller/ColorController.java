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
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.service.ColorService;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;

@RestController
@RequestMapping("/colors")
@Validated
@Tag(name = "Colors", description = "API for managing build colors")
public class ColorController {

    private final ColorService colorService;

    public ColorController(ColorService colorService) {
        this.colorService = colorService;
    }

    @Operation(summary = "Create a new color", description = "Creates a new color resource.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201",
            description = "Color created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Color.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input "
            + "(e.g., blank name, "
                    + "duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                                    .class})))
    })
    @PostMapping
    public ResponseEntity<Color> createColor(
            @Parameter(description = "Name of the new color", required = true, example = "DarkOak")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String name) {
        Color color = colorService.createColor(name);
        return new ResponseEntity<>(color, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all colors", description = "Retrieves a list of all colors with "
            + "their related builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved colors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto[].class)))
    })
    @GetMapping
    public ResponseEntity<List<ColorDto>> getAllColors() {
        List<ColorDto> colors = colorService.findAllColorDtos();
        return ResponseEntity.ok(colors);
    }

    @Operation(summary = "Get color by identifier", description = "Retrieves a specific color "
            + "by its ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved color",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto.class))), @ApiResponse(
                                    responseCode = "404", description = "Color not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<ColorDto> getColorByIdentifier(
            @Parameter(description = "ID or exact name of the color", required = true,
                    example = "5 or DarkOak")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        ColorDto colorDto = findColorDtoByIdentifier(identifier);
        return ResponseEntity.ok(colorDto);
    }

    @Operation(summary = "Update a color's name", description = "Updates the name of an "
            + "existing color identified by ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Color updated "
            + "successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Color.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (e.g., "
            + "blank name, duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class, ProblemDetail
                                    .class}))), @ApiResponse(responseCode = "404",
            description = "Color not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{identifier}")
    public ResponseEntity<Color> updateColor(
            @Parameter(description = "ID or exact name of the color to update", required = true,
                    example = "5 or DarkOak")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the color", required = true,
                    example = "SprucePlanks")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            @NotPurelyNumeric(message = StringConstants.NAME_NOT_ONLY_NUMERIC)
            String newName) {
        Color color = colorService.findColors(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Color updatedColor = colorService.updateColor(color.getId(), newName);
        return ResponseEntity.ok(updatedColor);
    }

    @Operation(summary = "Delete a color", description = "Deletes a color by ID or exact name. "
            + "Fails if the color is associated with any builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204",
            description = "Color deleted successfully", content = @Content), @ApiResponse
            (responseCode = "404", description = "Color not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "409", description = "Color is associated with "
            + "builds and cannot be deleted",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteColor(
            @Parameter(description = "ID or exact name of the color to delete", required = true,
                    example = "5 or DarkOak")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            colorService.deleteColor(colorId);
        } catch (NumberFormatException e) {
            colorService.deleteColorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find colors by name query", description = "Finds colors using a "
            + "case-insensitive fuzzy name match (via SIMILARITY).")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully found "
            + "colors (list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ColorDto[].class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid query "
            + "parameter provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<ColorDto>> getColorsByQuery(
            @Parameter(description = "Fuzzy name to search for colors.", example = "Oak")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name
            /* @RequestParam Map<String, String> allParams - Reverted */) {

        List<ColorDto> colors = colorService.findColorDtos(name);
        return ResponseEntity.ok(colors);
    }

    // Helper method - no Swagger needed
    private ColorDto findColorDtoByIdentifier(String identifier) {
        try {
            Long colorId = Long.valueOf(identifier);
            return colorService.findColorDtoById(colorId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<ColorDto> colors = colorService.findColorDtos(identifier);
            return colors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.COLOR, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}