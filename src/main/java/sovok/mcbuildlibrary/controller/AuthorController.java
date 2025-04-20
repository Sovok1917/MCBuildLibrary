// file: src/main/java/sovok/mcbuildlibrary/controller/AuthorController.java
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

import java.util.List;

@RestController
@RequestMapping(StringConstants.AUTHORS_ENDPOINT) // Define specific path here
@Tag(name = StringConstants.AUTHORS, description = "API for managing build authors") // Specific Tag
public class AuthorController extends BaseNamedEntityController<Author, AuthorDto, AuthorService> {

    @Autowired // Constructor injection via base class
    public AuthorController(AuthorService authorService) {
        super(authorService);
    }

    // --- Implement abstract methods from base controller ---

    @Override
    protected String getEntityTypeName() {
        return StringConstants.AUTHOR;
    }

    @Override
    protected String getEntityTypePluralName() {
        return StringConstants.AUTHORS;
    }

    @Override
    protected String getEntityNameExample() {
        return "BuilderBob"; // Specific example
    }

    @Override
    protected String getEntityIdentifierExample() {
        return "1 or BuilderBob"; // Specific example
    }

    // --- Override endpoints only if specific annotations/logic needed ---

    @Override
    @PostMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Author created successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Author.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (blank name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception.ValidationErrorResponse.class, org.springframework.http.ProblemDetail.class})))
    })
    public ResponseEntity<Author> createEntity(
            @Parameter(description = "Name of the new author", required = true, example = "BuilderBob")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String name) {
        return super.createEntity(name);
    }

    @Override
    @GetMapping
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved authors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto[].class)))
    })
    public ResponseEntity<List<AuthorDto>> getAllEntities() {
        return super.getAllEntities();
    }

    @Override
    @GetMapping("/{identifier}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved author",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<AuthorDto> getEntityByIdentifier(
            @Parameter(description = "ID or exact name of the author", required = true, example = "1 or BuilderBob")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        return super.getEntityByIdentifier(identifier);
    }

    @Override
    @PutMapping("/{identifier}")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Author.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input (blank name, duplicate name, etc.)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {sovok.mcbuildlibrary.exception.ValidationErrorResponse.class, org.springframework.http.ProblemDetail.class}))),
            @ApiResponse(responseCode = "404", description = "Author not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<Author> updateEntity(
            @Parameter(description = "ID or exact name of the author to update", required = true, example = "1 or BuilderBob")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the author", required = true, example = "ArchitectAnna")
            @RequestParam(StringConstants.NAME_REQ_PARAM) String newName) {
        return super.updateEntity(identifier, newName);
    }

    @Override
    @GetMapping("/query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully found authors (list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto[].class))),
            @ApiResponse(responseCode = "400", description = "Invalid query parameter provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = org.springframework.http.ProblemDetail.class)))
    })
    public ResponseEntity<List<AuthorDto>> getEntitiesByQuery(
            @Parameter(description = "Fuzzy name to search for authors.", example = "Bldr")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name) {
        return super.getEntitiesByQuery(name);
    }

    // Delete uses base implementation
}