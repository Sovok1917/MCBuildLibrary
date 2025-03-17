package sovok.mcbuildlibrary.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sovok.mcbuildlibrary.exception.InvalidQueryParameterException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.service.AuthorService;

import java.util.List;

@RestController
@RequestMapping("/authors")
public class AuthorController {

    private final AuthorService authorService;

    public AuthorController(AuthorService authorService) {
        this.authorService = authorService;
    }

    // ... other methods unchanged ...

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuthor(@PathVariable String id) {
        try {
            Long authorId = Long.valueOf(id);
            authorService.deleteAuthor(authorId);
            return ResponseEntity.noContent().build();
        } catch (NumberFormatException e) {
            throw new InvalidQueryParameterException("Invalid ID format: " + id);
        }
    }
}

record AuthorRequest(String name) {}