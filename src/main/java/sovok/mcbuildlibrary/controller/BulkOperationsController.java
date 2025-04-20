// file: src/main/java/sovok/mcbuildlibrary/controller/BulkOperationsController.java
package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.function.Function; // Import Function
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.BulkCreateRequestDto;
import sovok.mcbuildlibrary.dto.BulkCreateResponseDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.service.AuthorService;
import sovok.mcbuildlibrary.service.ColorService;
import sovok.mcbuildlibrary.service.ThemeService;
import sovok.mcbuildlibrary.util.BulkCreationResult;


@RestController
@RequestMapping(StringConstants.BULK_OPERATIONS_ENDPOINT)
@Validated
@Tag(name = StringConstants.BULK_TAG_NAME, description = StringConstants.BULK_TAG_DESCRIPTION)
public class BulkOperationsController {

    private final AuthorService authorService;
    private final ThemeService themeService;
    private final ColorService colorService;

    public BulkOperationsController(AuthorService authorService, ThemeService themeService,
                                    ColorService colorService) {
        this.authorService = authorService;
        this.themeService = themeService;
        this.colorService = colorService;
    }

    @Operation(summary = StringConstants.BULK_CREATE_SUMMARY, description
            = StringConstants.BULK_CREATE_DESCRIPTION)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description
            = StringConstants.BULK_OPERATION_SUCCESS,
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = BulkCreateResponseDto.class))),
            @ApiResponse(responseCode = "400", description = StringConstants.VALIDATION_FAILED_MESSAGE,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ValidationErrorResponse.class)))
    })
    @PostMapping(value = StringConstants.BULK_CREATE_METADATA_ENDPOINT, consumes
            = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulkCreateResponseDto> createMetadataBulk(
            @Parameter(description = "Lists of names for Authors, Themes, and Colors to create.",
                    required = true)
            @Valid @RequestBody BulkCreateRequestDto requestDto) {

        // *** FIX: Use the inherited createBulk method reference ***
        BulkCreationResult<String> authorResult = processBulkCreation(requestDto.getAuthors(),
                authorService::createBulk); // Use base method
        BulkCreationResult<String> themeResult = processBulkCreation(requestDto.getThemes(),
                themeService::createBulk); // Use base method
        BulkCreationResult<String> colorResult = processBulkCreation(requestDto.getColors(),
                colorService::createBulk); // Use base method

        BulkCreateResponseDto response = BulkCreateResponseDto.builder()
                .createdAuthors(authorResult.createdItems())
                .skippedAuthors(authorResult.skippedItems())
                .createdThemes(themeResult.createdItems())
                .skippedThemes(themeResult.skippedItems())
                .createdColors(colorResult.createdItems())
                .skippedColors(colorResult.skippedItems())
                .build();

        return ResponseEntity.ok(response);
    }

    // Helper method to handle null lists and extract names
    private BulkCreationResult<String> processBulkCreation(
            List<BulkCreateRequestDto.NameDto> nameDtos,
            // Use Function interface explicitly
            Function<List<String>, BulkCreationResult<String>> creationFunction) {

        if (nameDtos == null || nameDtos.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), Collections.emptyList());
        }

        List<String> names = nameDtos.stream()
                .map(BulkCreateRequestDto.NameDto::name) // Use record accessor method
                .toList(); // Use toList() for immutable list
        return creationFunction.apply(names);
    }
}