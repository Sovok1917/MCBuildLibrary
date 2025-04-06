// file: src/main/java/sovok/mcbuildlibrary/controller/AuthorController.java
package sovok.mcbuildlibrary.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Import
import java.util.Set;
import org.springframework.http.HttpStatus;
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
// Removed import for ResourceNotFoundException
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

@RestController
@RequestMapping("/authors")
@Validated
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    public ResponseEntity<Author> createAuthor(
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String name) {
        // Service now throws IllegalArgumentException if duplicate
        Author author = authorService.createAuthor(name);
        return new ResponseEntity<>(author, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        List<AuthorDto> authors = authorService.findAllAuthorDtos();
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<AuthorDto> getAuthorByIdentifier(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Helper method now throws NoSuchElementException
        AuthorDto authorDto = findAuthorDtoByIdentifier(identifier);
        return ResponseEntity.ok(authorDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Author> updateAuthor(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier,
            @RequestParam(StringConstants.NAME_REQ_PARAM)
            @NotBlank(message = StringConstants.NAME_NOT_BLANK)
            @Size(min = 2, message = StringConstants.NAME_SIZE)
            String newName) {
        // Service find/update methods now handle not found (NoSuchElementException)
        // and duplicate name checks (IllegalArgumentException)
        Author author = authorService.findAuthors(identifier).stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                StringConstants.AUTHOR, StringConstants.WITH_NAME, identifier,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Author updatedAuthor = authorService.updateAuthor(author.getId(), newName);
        return ResponseEntity.ok(updatedAuthor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(
            @PathVariable(StringConstants.IDENTIFIER_PATH_VAR) String identifier) {
        // Service delete methods now handle not found (NoSuchElementException)
        // and conflicts (IllegalStateException)
        try {
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<AuthorDto>> getAuthorsByQuery(
            @RequestParam Map<String, String> allParams) {

        validateQueryParameters(allParams, StringConstants.ALLOWED_SIMPLE_QUERY_PARAMS);
        String name = allParams.get(StringConstants.NAME_REQ_PARAM);

        List<AuthorDto> authors = authorService.findAuthorDtos(name);
        if (authors.isEmpty() && name != null) { // Only throw 404 if a specific query yielded no results
            throw new NoSuchElementException( // Changed from ResourceNotFoundException
                    String.format(StringConstants.QUERY_NO_RESULTS, StringConstants.AUTHORS, name));
        }
        // If no name provided or name query successful (even if empty), return OK
        return ResponseEntity.ok(authors);
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

    private AuthorDto findAuthorDtoByIdentifier(String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            return authorService.findAuthorDtoById(authorId)
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.AUTHOR, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            List<AuthorDto> authors = authorService.findAuthorDtos(identifier);
            return authors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException( // Changed from ResourceNotFoundException
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    StringConstants.AUTHOR, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
}