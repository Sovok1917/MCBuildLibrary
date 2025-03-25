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
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
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

    @GetMapping("/{id}")
    public ResponseEntity<Author> getAuthorById(@PathVariable String id) {
        try {
            Long authorId = Long.valueOf(id);
            Author author = authorService.findAuthorById(authorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + id
                            + " not found"));
            return ResponseEntity.ok(author);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException(ErrorMessages.INVALID_ID_FORMAT_MESSAGE + id);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Author> updateAuthor(@PathVariable String id, @RequestParam("name")
        String name) {
        try {
            Long authorId = Long.valueOf(id);
            Author updatedAuthor = authorService.updateAuthor(authorId, name);
            return ResponseEntity.ok(updatedAuthor);
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException(ErrorMessages.INVALID_ID_FORMAT_MESSAGE + id);
        }
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable String identifier) {
        try {
            Long authorId = Long.valueOf(identifier);
            authorService.deleteAuthor(authorId);
        } catch (NumberFormatException e) {
            // If it's not a valid Long, treat it as a name
            authorService.deleteAuthorByName(identifier);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public ResponseEntity<List<Author>> getAuthorsByQuery(@RequestParam(required = false)
                                                              String name) {
        List<Author> authors = authorService.findAuthors(name);
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException("No authors found matching the query");
        }
        return ResponseEntity.ok(authors);
    }
}