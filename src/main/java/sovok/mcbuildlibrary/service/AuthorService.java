package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
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
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByAuthorId(author.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
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

    public Optional<AuthorDto> findAuthorDtoById(Long id) {
        return authorRepository.findById(id).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAllAuthorDtos() {
        List<Author> authors = authorRepository.findAll();
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException("No authors are currently available");
        }
        return authors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAuthorDtos(String name) {
        List<Author> authors = authorRepository.fuzzyFindByName(name);
        return authors.stream().map(this::convertToDto).toList();
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
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
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
}