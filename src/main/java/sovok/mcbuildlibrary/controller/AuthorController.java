package sovok.mcbuildlibrary.controller;

import io.swagger.v3.oas.annotations.Operation; // Import OpenAPI annotations
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
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.exception.ValidationErrorResponse;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

@RestController
@RequestMapping("/authors")
@Validated
@Tag(name = "Authors", description = "API for managing build authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @Operation(summary = "Create a new author", description = "Creates a new author resource.")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Author created "
            + "successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Author.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input (e.g., "
            + "blank name, "
                    + "name too short, duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class,
                                ProblemDetail.class})))
    })

    @PostMapping
    public ResponseEntity<Author> createAuthor(
            @Parameter(description = "Name of the new author", required = true,
                    example = "BuilderBob")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String name) {
        Author author = authorService.createAuthor(name);
        return new ResponseEntity<>(author, HttpStatus.CREATED);
    }

    @Operation(summary = "Get all authors", description = "Retrieves a list of all authors "
            + "with their related builds.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved authors",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto[].class)))
    })
    @GetMapping
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        List<AuthorDto> authors = authorService.findAllAuthorDtos();
        return ResponseEntity.ok(authors);
    }

    @Operation(summary = "Get author by identifier", description = "Retrieves a specific "
            + "author by their ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Successfully retrieved author",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto.class))), @ApiResponse(
                                    responseCode = "404", description = "Author not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{identifier}")
    public ResponseEntity<AuthorDto> getAuthorByIdentifier(
            @Parameter(description = "ID or exact name of the author", required = true,
                    example = "1 or BuilderBob")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        AuthorDto authorDto = findAuthorDtoByIdentifier(identifier);
        return ResponseEntity.ok(authorDto);
    }

    @Operation(summary = "Update an author's name", description = "Updates the name of an "
            + "existing author identified by ID or exact name.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200",
            description = "Author updated successfully",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = Author.class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid input "
            + "(e.g., blank name, "
                    + "name too short, duplicate name)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(oneOf = {ValidationErrorResponse.class,
                                ProblemDetail.class}))), @ApiResponse(responseCode = "404",
            description = "Author not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{identifier}")
    public ResponseEntity<Author> updateAuthor(
            @Parameter(description = "ID or exact name of the author to update", required = true,
                    example = "1 or BuilderBob")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @Parameter(description = "The new name for the author", required = true,
                    example = "ArchitectAnna")
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String newName) {
        Author author = authorService.findAuthors(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.AUTHOR, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Author updatedAuthor = authorService.updateAuthor(author.getId(), newName);
        return ResponseEntity.ok(updatedAuthor);
    }

    @Operation(summary = "Delete an author", description = "Deletes an author by ID or exact "
            + "name. Also removes the author from associated builds or deletes builds if it's "
            + "the last author.")
    @ApiResponses(value = {@ApiResponse(responseCode = "204",
            description = "Author deleted successfully",
                    content = @Content), @ApiResponse(responseCode = "404",
            description = "Author not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(
            @Parameter(description = "ID or exact name of the author to delete", required = true,
                    example = "1 or BuilderBob")
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Find authors by name query", description = "Finds authors using a "
            + "case-insensitive fuzzy name match (via SIMILARITY).")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully found "
            + "authors (list might be empty)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = AuthorDto[].class))), @ApiResponse(
                                    responseCode = "400", description = "Invalid query parameter "
            + "provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class))), @ApiResponse(
                                    responseCode = "404", description = "No authors found for "
            + "the specific "
                    + "query provided",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/query")
    public ResponseEntity<List<AuthorDto>> getAuthorsByQuery(
            // Revert back from Map to explicit @RequestParam for Swagger documentation
            @Parameter(description = "Fuzzy name to search for authors.", example = "Bldr")
            @RequestParam(value = StringConstants.NAME_REQ_PARAM, required = false) String name
            /* @RequestParam Map<String, String> allParams - Reverted this */) {

        List<AuthorDto> authors = authorService.findAuthorDtos(name);
        if (authors.isEmpty() && name != null) {
            throw new NoSuchElementException(
                    String.format(StringConstants.QUERY_NO_RESULTS, StringConstants.AUTHORS, name));
        }
        return ResponseEntity.ok(authors);
    }

    // Helper method - not an endpoint, no Swagger annotations needed
    private AuthorDto findAuthorDtoByIdentifier(String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            return authorService.findAuthorDtoById(authorId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.AUTHOR, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<AuthorDto> authors = authorService.findAuthorDtos(identifier);
            return authors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.AUTHOR, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}