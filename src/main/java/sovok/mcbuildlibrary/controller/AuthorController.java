// file: src/main/java/sovok/mcbuildlibrary/controller/AuthorController.java
package sovok.mcbuildlibrary.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private static final String IDENTIFIER_PATH_VAR = "identifier";
    private static final String NAME_REQ_PARAM = "name";

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    @PostMapping
    public ResponseEntity<Author> createAuthor(@RequestParam(NAME_REQ_PARAM) String name) {
        // Create returns the entity, which might not be cached directly, but service method caches it
        Author author = authorService.createAuthor(name);
        return new ResponseEntity<>(author, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        // Service method handles caching and "not found" for empty initial list
        List<AuthorDto> authors = authorService.findAllAuthorDtos();
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<AuthorDto> getAuthorByIdentifier(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        // Helper method uses cached service methods
        AuthorDto authorDto = findAuthorDtoByIdentifier(identifier);
        return ResponseEntity.ok(authorDto);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Author> updateAuthor(@PathVariable(IDENTIFIER_PATH_VAR) String identifier,
                                               @RequestParam(NAME_REQ_PARAM) String newName) {
        // Find first to get ID if identifier is name
        // This findAuthors is not cached, but the subsequent updateAuthor call handles cache updates
        Author author = authorService.findAuthors(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                ErrorMessages.AUTHOR, ErrorMessages.WITH_NAME, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));

        // Update returns entity, service method caches the update
        Author updatedAuthor = authorService.updateAuthor(author.getId(), newName);
        return ResponseEntity.ok(updatedAuthor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable(IDENTIFIER_PATH_VAR) String identifier) {
        // Service methods handle cache eviction
        try {
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<AuthorDto>> getAuthorsByQuery(@RequestParam(required = false, value = NAME_REQ_PARAM)
                                                             String name) {
        // Fuzzy find in service is not cached
        List<AuthorDto> authors = authorService.findAuthorDtos(name);
        if (authors.isEmpty()) {
            // Throw here for query specifically
            throw new ResourceNotFoundException(
                    String.format(ErrorMessages.QUERY_NO_RESULTS, "authors", (name != null ? name : "<all>")));
        }
        return ResponseEntity.ok(authors);
    }

    // Helper uses cached DTO find methods from service
    private AuthorDto findAuthorDtoByIdentifier(String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            // findAuthorDtoById uses cache
            return authorService.findAuthorDtoById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                    ErrorMessages.AUTHOR, ErrorMessages.WITH_ID, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            // findAuthorDtos (fuzzy) is not cached, but we filter locally
            List<AuthorDto> authors = authorService.findAuthorDtos(identifier); // Not cached
            // We need precise match for identifier lookup
            return authors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                                    ErrorMessages.AUTHOR, ErrorMessages.WITH_NAME, identifier, ErrorMessages.NOT_FOUND_MESSAGE)));
        }
    }
}