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
import sovok.mcbuildlibrary.dto.AuthorDto; // Import DTO
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    // Create still returns the basic Author entity
    @PostMapping
    public ResponseEntity<Author> createAuthor(@RequestParam("name") String name) {
        Author author = authorService.createAuthor(name);
        return new ResponseEntity<>(author, HttpStatus.CREATED);
    }

    // --- Modified to return List<AuthorDto> ---
    @GetMapping
    public ResponseEntity<List<AuthorDto>> getAllAuthors() {
        // Exception handling for empty list is in the service now,
        // but you could move it here if preferred.
        List<AuthorDto> authors = authorService.findAllAuthorDtos();
        return ResponseEntity.ok(authors);
    }
    // --- End Modification ---

    // --- Modified to return AuthorDto ---
    @GetMapping("/{identifier}")
    public ResponseEntity<AuthorDto> getAuthorByIdentifier(@PathVariable String identifier) {
        AuthorDto authorDto = findAuthorDtoByIdentifier(identifier);
        return ResponseEntity.ok(authorDto);
    }
    // --- End Modification ---

    // Update still returns the basic Author entity
    @PutMapping("/{identifier}")
    public ResponseEntity<Author> updateAuthor(@PathVariable String identifier,
                                               @RequestParam("name") String newName) {
        Author author = authorService.findAuthors(identifier).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Author " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE)); // Simplified find logic

        Author updatedAuthor = authorService.updateAuthor(author.getId(), newName);
        return ResponseEntity.ok(updatedAuthor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable String identifier) {
        // Deletion logic remains the same, uses internal service methods
        try {
            // Attempt to parse as Long first
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            // If not a Long, assume it's a name
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    // --- Modified to return List<AuthorDto> ---
    @GetMapping("/query")
    public ResponseEntity<List<AuthorDto>> getAuthorsByQuery(@RequestParam(required = false)
                                                                 String name) {
        List<AuthorDto> authors = authorService.findAuthorDtos(name);
        if (authors.isEmpty()) {
            // Check for empty list here, as service doesn't throw for query
            throw new ResourceNotFoundException("No authors found matching the query: "
                    + (name != null ? name : "<all>"));
        }
        return ResponseEntity.ok(authors);
    }

    private AuthorDto findAuthorDtoByIdentifier(String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            return authorService.findAuthorDtoById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            // If not an ID, treat as name (assuming name is unique)
            List<AuthorDto> authors = authorService.findAuthorDtos(identifier);
            if (authors.isEmpty()) {
                throw new ResourceNotFoundException("Author with name '" + identifier + "' "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            // Since name should be unique, return the first one found
            return authors.stream()
                    .filter(dto -> dto.name().equalsIgnoreCase(identifier))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Author with name '"
                            + identifier + "' "
                            + ErrorMessages.NOT_FOUND_MESSAGE));
        }
    }
}