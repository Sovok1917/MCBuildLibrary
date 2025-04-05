// file: src/main/java/sovok/mcbuildlibrary/service/AuthorService.java
package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache; // Import Cache
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);
    private static final String CACHE_ENTITY_TYPE = ErrorMessages.AUTHOR;

    private final AuthorRepository authorRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache; // Inject Cache

    public AuthorService(AuthorRepository authorRepository, BuildRepository buildRepository, InMemoryCache cache) {
        this.authorRepository = authorRepository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    private AuthorDto convertToDto(Author author) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByAuthorId(author.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
    }

    // Used internally by BuildService, caching might not be beneficial here unless called frequently outside build creation
    @Transactional
    public Author findOrCreateAuthor(String name) {
        return authorRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Author '{}' not found, creating new.", name);
                    Author newAuthor = Author.builder().name(name).build();
                    Author savedAuthor = authorRepository.save(newAuthor);
                    // Optionally cache here if needed, but primarily POST/PUT manage cache
                    // cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedAuthor.getId()), savedAuthor);
                    // cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
                    return savedAuthor;
                });
    }

    @Transactional
    public Author createAuthor(String name) {
        Optional<Author> existingAuthor = authorRepository.findByName(name);
        if (existingAuthor.isPresent()) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }
        Author author = Author.builder().name(name).build();
        Author savedAuthor = authorRepository.save(author);
        logger.info("Created Author with ID: {}", savedAuthor.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedAuthor.getId()), savedAuthor);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
        // --- End Cache Modification ---

        return savedAuthor;
    }

    @Transactional(readOnly = true) // Make read-only
    public Optional<AuthorDto> findAuthorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);

        // --- Cache Read ---
        Optional<Author> cachedAuthor = cache.get(cacheKey);
        if (cachedAuthor.isPresent()) {
            return cachedAuthor.map(this::convertToDto);
        }
        // --- End Cache Read ---

        Optional<Author> authorOpt = authorRepository.findById(id);
        authorOpt.ifPresent(author -> cache.put(cacheKey, author)); // Cache if found
        return authorOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAllAuthorDtos() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);

        // --- Cache Read ---
        Optional<List<Author>> cachedAuthors = cache.get(cacheKey);
        if (cachedAuthors.isPresent()) {
            // Important: Convert cached *entities* to DTOs
            return cachedAuthors.get().stream().map(this::convertToDto).toList();
        }
        // --- End Cache Read ---

        List<Author> authors = authorRepository.findAll();
        if (authors.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ErrorMessages.NO_ENTITIES_AVAILABLE, "authors"));
        }

        // --- Cache Write ---
        cache.put(cacheKey, authors); // Cache the list of entities
        // --- End Cache Write ---

        return authors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAuthorDtos(String name) {
        // Fuzzy search results are generally not good candidates for simple caching
        // unless the exact query parameters are used as keys, which adds complexity.
        // Skipping cache for this fuzzy find method for simplicity.
        List<Author> authors = authorRepository.fuzzyFindByName(name);
        // Don't throw exception here, let controller handle empty list for query
        return authors.stream().map(this::convertToDto).toList();
    }

    // This method is used internally for PUT/DELETE by identifier (if name)
    // Caching is less critical here as it precedes a write operation (update/delete)
    // But we can add caching for the underlying repository call if desired.
    @Transactional(readOnly = true)
    public List<Author> findAuthors(String name) {
        // Exact match findByName - potential cache candidate if used often standalone
        Optional<Author> authorOpt = authorRepository.findByName(name);
        return authorOpt.map(List::of).orElseGet(List::of); // Return list for consistency
    }


    @Transactional
    public Author updateAuthor(Long id, String newName) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));

        // Check if new name is already used by another author
        Optional<Author> authorWithSameName = authorRepository.findByName(newName);
        if (authorWithSameName.isPresent() && !authorWithSameName.get().getId().equals(id)) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, newName, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }

        author.setName(newName);
        Author updatedAuthor = authorRepository.save(author);
        logger.info("Updated Author with ID: {}", updatedAuthor.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedAuthor.getId()), updatedAuthor); // Update cache
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
        // --- End Cache Modification ---

        return updatedAuthor;
    }

    // Separated internal logic for reuse
    @Transactional
    private void deleteAuthorInternal(Author author) {
        // Delete associated builds ONLY if this author is the *last* author
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        for (Build build : builds) {
            // Eager fetch or check size if lazy - ensure authors are loaded
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                logger.warn("Deleting Build ID {} as its last author {} (ID {}) is being deleted.",
                        build.getId(), author.getName(), author.getId());
                // Evict build from cache BEFORE deleting it
                cache.evict(InMemoryCache.generateKey(ErrorMessages.BUILD, build.getId()));
                cache.evict(InMemoryCache.generateGetAllKey(ErrorMessages.BUILD));
                buildRepository.delete(build);
            } else {
                // Just remove the author association
                build.getAuthors().remove(author);
                Build updatedBuild = buildRepository.save(build);
                // Update the build in cache
                cache.put(InMemoryCache.generateKey(ErrorMessages.BUILD, updatedBuild.getId()), updatedBuild);
                cache.evict(InMemoryCache.generateGetAllKey(ErrorMessages.BUILD)); // Invalidate build list
            }
        }

        Long authorId = author.getId(); // Get ID before deleting
        authorRepository.delete(author);
        logger.info("Deleted Author with ID: {}", authorId);

        // --- Cache Modification ---
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, authorId)); // Evict deleted author
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
        // --- End Cache Modification ---
    }

    @Transactional
    public void deleteAuthor(Long id) {
        // Fetch first to ensure it exists and for cache eviction key
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteAuthorInternal(author);
    }

    @Transactional
    public void deleteAuthorByName(String name) {
        // Fetch first to ensure it exists and for cache eviction key (ID)
        Author author = authorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteAuthorInternal(author);
    }
}