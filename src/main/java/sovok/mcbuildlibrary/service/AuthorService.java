package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class AuthorService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.AUTHOR;

    private final AuthorRepository authorRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache;


    public AuthorService(AuthorRepository authorRepository, BuildRepository buildRepository,
                         InMemoryCache cache) {
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

    @Transactional
    public Author findOrCreateAuthor(String name) {
        return authorRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Author '{}' not found, creating new.", name);
                    Author newAuthor = Author.builder().name(name).build();
                    return authorRepository.save(newAuthor);
                });
    }

    @Transactional
    public Author createAuthor(String name) {
        Optional<Author> existingAuthor = authorRepository.findByName(name);
        if (existingAuthor.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }
        Author author = Author.builder().name(name).build();
        Author savedAuthor = authorRepository.save(author);
        logger.info("Created Author with ID: {}", savedAuthor.getId());

        // Cache the created entity by ID (still useful)
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedAuthor.getId()), savedAuthor);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Keep query cache eviction

        return savedAuthor;
    }

    @Transactional(readOnly = true)
    public Optional<AuthorDto> findAuthorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Author> cachedAuthor = cache.get(cacheKey);
        if (cachedAuthor.isPresent()) {
            return cachedAuthor.map(this::convertToDto);
        }

        Optional<Author> authorOpt = authorRepository.findById(id);
        // Still cache individual finds by ID
        authorOpt.ifPresent(author -> cache.put(cacheKey, author));
        return authorOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAllAuthorDtos() {

        logger.debug("Fetching all authors from repository (getAll cache disabled).");
        List<Author> authors = authorRepository.findAll();

        return authors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AuthorDto> findAuthorDtos(String name) {
        // Query caching remains
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Author>> cachedResult = cache.get(queryKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get().stream().map(this::convertToDto).toList();
        }

        List<Author> authors = authorRepository.fuzzyFindByName(name);
        cache.put(queryKey, authors); // Cache query result
        return authors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Author> findAuthors(String name) {
        Optional<Author> authorOpt = authorRepository.findByName(name);
        return authorOpt.map(List::of).orElseGet(List::of);
    }

    @Transactional
    public Author updateAuthor(Long id, String newName) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        Optional<Author> authorWithSameName = authorRepository.findByName(newName);
        if (authorWithSameName.isPresent() && !authorWithSameName.get().getId().equals(id)) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, newName,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        author.setName(newName);
        Author updatedAuthor = authorRepository.save(author);
        logger.info("Updated Author with ID: {}", updatedAuthor.getId());

        // Update cache for this specific item by ID
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedAuthor.getId()),
                updatedAuthor);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Keep query cache eviction

        return updatedAuthor;
    }

    private void deleteAuthorInternal(Author author) {
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        boolean buildCacheInvalidated = false;

        for (Build build : builds) {
            buildCacheInvalidated = true;
            // ... (build deletion/update logic remains the same) ...
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                logger.warn("Deleting Build ID {} as its last author {} (ID {}) is being deleted.",
                        build.getId(), author.getName(), author.getId());
                cache.evict(InMemoryCache.generateKey(StringConstants.BUILD, build.getId()));
                buildRepository.delete(build);
            } else {
                build.getAuthors().remove(author);
                Build updatedBuild = buildRepository.save(build);
                cache.put(InMemoryCache.generateKey(StringConstants.BUILD, updatedBuild.getId()),
                        updatedBuild);
            }
        }

        Long authorId = author.getId();
        authorRepository.delete(author);
        logger.info("Deleted Author with ID: {}", authorId);

        // Evict the deleted author from cache
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, authorId));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Keep query cache eviction

        // Evict build caches if builds were affected
        if (buildCacheInvalidated) {
            cache.evictQueryCacheByType(StringConstants.BUILD); // Keep build query eviction
        }
    }

    @Transactional
    public void deleteAuthor(Long id) {
        Author author = authorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteAuthorInternal(author);
    }

    @Transactional
    public void deleteAuthorByName(String name) {
        Author author = authorRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteAuthorInternal(author);
    }
}