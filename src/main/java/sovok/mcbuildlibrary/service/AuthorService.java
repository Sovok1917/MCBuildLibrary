package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.repository.AuthorRepository;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    public Author findOrCreateAuthor(String name) {
        Optional<Author> authorOpt = authorRepository.findByName(name);
        if (authorOpt.isPresent()) {
            return authorOpt.get();
        } else {
            Author newAuthor = Author.builder().name(name).build();
            return authorRepository.save(newAuthor);
        }
    }

    public Author createAuthor(String name) {
        if (authorRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException(
                    "Author with name '" + name + "' already exists");
        }
        Author author = Author.builder().name(name).build();
        return authorRepository.save(author);
    }

    public Optional<Author> findAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    public List<Author> findAllAuthors() {
        List<Author> authors = authorRepository.findAll();
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException("No authors are currently available");
        }
        return authors;
    }

    public Author updateAuthor(Long id, String newName) {
        return authorRepository.findById(id)
                .map(author -> {
                    if (authorRepository.findByName(newName).isPresent()
                            && !author.getName().equals(newName)) {
                        throw new IllegalArgumentException(
                                "Another author with name '" + newName + "' already exists");
                    }
                    author.setName(newName);
                    return authorRepository.save(author);
                })
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Author with ID " + id + " not found"));
    }

    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new ResourceNotFoundException("Author with ID " + id + " not found");
        }
        authorRepository.deleteById(id); // Cascading deletes builds and screenshots
    }
}