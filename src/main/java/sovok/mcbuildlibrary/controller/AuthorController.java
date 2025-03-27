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

    @PostMapping
    public ResponseEntity<Author> createAuthor(@RequestParam("name") String name) {
        Author author = authorService.createAuthor(name);
        return new ResponseEntity<>(author, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Author>> getAllAuthors() {
        List<Author> authors = authorService.findAllAuthors();
        return ResponseEntity.ok(authors);
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Author> getAuthorByIdentifier(@PathVariable String identifier) {
        Author author = findAuthorByIdentifier(identifier);
        return ResponseEntity.ok(author);
    }

    @PutMapping("/{identifier}")
    public ResponseEntity<Author> updateAuthor(@PathVariable String identifier,
                                               @RequestParam("name") String newName) {
        Author author = findAuthorByIdentifier(identifier);
        Author updatedAuthor = authorService.updateAuthor(author.getId(), newName);
        return ResponseEntity.ok(updatedAuthor);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<Author>> getAuthorsByQuery(@RequestParam(required = false) String
                                                                      name) {
        List<Author> authors = authorService.findAuthors(name);
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException("No authors found matching the query");
        }
        return ResponseEntity.ok(authors);
    }

    private Author findAuthorByIdentifier(String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            return authorService.findAuthorById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + identifier
                            + " " + ErrorMessages.NOT_FOUND_MESSAGE));
        } catch (NumberFormatException e) {
            List<Author> authors = authorService.findAuthors(identifier);
            if (authors.isEmpty()) {
                throw new ResourceNotFoundException("Author with name " + identifier + " "
                        + ErrorMessages.NOT_FOUND_MESSAGE);
            }
            return authors.get(0); // Name is unique, so list has at most one element
        }
    }
}