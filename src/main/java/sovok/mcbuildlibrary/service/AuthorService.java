package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.dto.AuthorDto; // Import DTO
import sovok.mcbuildlibrary.dto.RelatedBuildDto; // Import DTO
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

    private AuthorDto convertToDto(Author author) {
        // Use the new repository method returning the projection
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByAuthorId(author.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(),
                        info.getName())) // Map projection to DTO
                .toList(); // SONAR FIX: Use toList() instead of collect(Collectors.toList())
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
    }

    public Author findOrCreateAuthor(String name) {
        // Keep returning Author entity for internal use (e.g., creating builds)
        Optional<Author> authorOpt = authorRepository.findByName(name);
        return authorOpt.orElseGet(() -> {
            Author newAuthor = Author.builder().name(name).build();
            return authorRepository.save(newAuthor);
        });
    }

    public Author createAuthor(String name) {
        // Return basic Author entity on creation
        Optional<Author> existingAuthor = authorRepository.findByName(name);
        if (existingAuthor.isPresent()) {
            throw new EntityInUseException("An author with the name '" + name
                    + "' already exists. Please choose a unique name.");
        }
        Author author = Author.builder().name(name).build();
        return authorRepository.save(author);
    }

    public Optional<AuthorDto> findAuthorDtoById(Long id) {
        return authorRepository.findById(id).map(this::convertToDto);
    }

    public List<AuthorDto> findAllAuthorDtos() {
        List<Author> authors = authorRepository.findAll();
        if (authors.isEmpty()) {
            // SONAR FIX: Removed commented-out code block
            throw new ResourceNotFoundException("No authors are currently available");
        }
        return authors.stream().map(this::convertToDto).toList(); // SONAR FIX: Use toList()
    }

    public List<AuthorDto> findAuthorDtos(String name) {
        List<Author> authors;
        if (name != null && !name.trim().isEmpty()) {
            String pattern = "%" + name.toLowerCase() + "%";
            authors = authorRepository.findByNameLike(pattern);
        } else {
            authors = authorRepository.findAll();
        }
        return authors.stream().map(this::convertToDto).toList(); // SONAR FIX: Use toList()
    }

    public List<Author> findAuthors(String name) {
        if (name != null) {
            String pattern = "%" + name.toLowerCase() + "%";
            return authorRepository.findByNameLike(pattern);
        } else {
            return authorRepository.findAll();
        }
    }


    public Author updateAuthor(Long id, String newName) {
        // Return basic Author entity on update
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

    // Deletion logic remains complex, ensure it handles Build relations correctly.
    // Consider implications if builds *must* have at least one author.
    private void deleteAuthorInternal(Author author) {
        // SONAR FIX: Removed unused local variable 'buildsWithAuthorOnly' and its assignment
        //.filter(b -> b.getAuthors().size() == 1) // Filter further if needed
        //.toList(); // Uncomment if needed

        // This logic might need refinement depending on business rules.
        // If a build MUST have an author, deleting the last author might require deleting the build
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        for (Build build : builds) {
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                // If this is the only author, delete the build
                buildRepository.delete(build);
            } else {
                // Otherwise, just remove the author from the build's author list
                build.getAuthors().remove(author);
                buildRepository.save(build); // Make sure to save the change
            }
        }
        // Delete the author itself after handling associations
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
}