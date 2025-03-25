package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BuildRepository buildRepository; // Add BuildRepository

    public AuthorService(AuthorRepository authorRepository, BuildRepository buildRepository) {
        this.authorRepository = authorRepository;
        this.buildRepository = buildRepository;
    }

    public Author findOrCreateAuthor(String name) {
        Optional<Author> authorOpt = authorRepository.findByName(name);
        return authorOpt.orElseGet(() -> {
            Author newAuthor = Author.builder().name(name).build();
            return authorRepository.save(newAuthor);
        });
    }

    public Author createAuthor(String name) {
        if (authorRepository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("Author with name '" + name + "' already exists");
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

    // Add this private method to encapsulate the deletion logic
    private void deleteAuthorInternal(Author author) {
        // Find all builds associated with this author
        List<Build> builds = buildRepository.filterBuilds(author.getName(), null, null, null, true);

        // Delete builds where this author is the only author
        for (Build build : builds) {
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                buildRepository.delete(build);
            } else {
                // Remove the author from builds with multiple authors
                build.getAuthors().remove(author);
                buildRepository.save(build);
            }
        }

        // Finally, delete the author
        authorRepository.delete(author);
    }

    // Update the existing deleteAuthor method
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + id
                        + " not found"));
        deleteAuthorInternal(author);
    }

    // Add the new deleteAuthorByName method
    public void deleteAuthorByName(String name) {
        Author author = authorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Author with name '" + name
                        + "' not found"));
        deleteAuthorInternal(author);
    }

    public List<Author> findAuthors(String name) {
        if (name != null) {
            return authorRepository.findByName(name).map(List::of).orElse(List.of());
        } else {
            return authorRepository.findAll();
        }
    }
}