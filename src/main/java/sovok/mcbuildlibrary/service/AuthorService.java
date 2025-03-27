package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class AuthorService {
    private final AuthorRepository authorRepository;
    private final BuildRepository buildRepository;

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
        Optional<Author> existingAuthor = authorRepository.findByName(name);
        if (existingAuthor.isPresent()) {
            throw new EntityInUseException("An author with the name '" + name
                    + "' already exists. Please choose a unique name.");
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
                    Optional<Author> authorWithSameName = authorRepository.findByName(newName);
                    if (authorWithSameName.isPresent()
                            && !authorWithSameName.get().getId().equals(id)) {
                        throw new EntityInUseException("An author with the name '" + newName
                                + "' already exists. Please choose a unique name.");
                    }
                    author.setName(newName);
                    return authorRepository.save(author);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + id
                        + " not found"));
    }

    private void deleteAuthorInternal(Author author) {
        List<Build> builds = buildRepository.filterBuilds(author.getName(), null, null, null, true);
        for (Build build : builds) {
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                buildRepository.delete(build);
            } else {
                build.getAuthors().remove(author);
                buildRepository.save(build);
            }
        }
        authorRepository.delete(author);
    }

    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Author with ID " + id
                        + " not found"));
        deleteAuthorInternal(author);
    }

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