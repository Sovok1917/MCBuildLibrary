package sovok.mcbuildlibrary.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import sovok.mcbuildlibrary.util.BulkCreationResult; // Import new Result class

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
                .toList(); // Use toList() for immutability
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
    }

    @Transactional
    public Author findOrCreateAuthor(String name) {
        // Consider caching findByName for performance if called frequently individually
        return authorRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Author '{}' not found, creating new.", name);
                    Author newAuthor = Author.builder().name(name).build();
                    Author savedAuthor = authorRepository.save(newAuthor);
                    // Cache the newly created author
                    cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedAuthor.getId()), savedAuthor);
                    cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Evict list/query caches
                    return savedAuthor;
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

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedAuthor.getId()), savedAuthor);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedAuthor;
    }

    /**
     * Creates multiple authors in bulk. Skips names that already exist (case-insensitive).
     *
     * @param namesToCreate A collection of author names to potentially create.
     * @return A BulkCreationResult containing lists of created and skipped names.
     */
    @Transactional
    public BulkCreationResult<String> createAuthorsBulk(Collection<String> namesToCreate) {
        if (namesToCreate == null || namesToCreate.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), Collections.emptyList());
        }

        // Normalize input names (e.g., trim, lower case for lookup) and remove duplicates
        Set<String> uniqueLowerNames = namesToCreate.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        if (uniqueLowerNames.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), namesToCreate.stream().toList()); // All were blank/null
        }

        // Find which names already exist
        Set<Author> existingAuthors = authorRepository.findByNamesIgnoreCase(uniqueLowerNames);
        Set<String> existingLowerNames = existingAuthors.stream()
                .map(Author::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        // Determine which original names need creation and which were skipped
        Map<Boolean, List<String>> partitionedNames = namesToCreate.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct() // Process distinct original names
                .collect(Collectors.partitioningBy(
                        name -> !existingLowerNames.contains(name.toLowerCase())
                ));

        List<String> namesToActuallyCreate = partitionedNames.get(true);
        List<String> skippedNames = partitionedNames.get(false);
        // Add any blank/null original names to skipped
        namesToCreate.stream()
                .filter(name -> name == null || name.isBlank())
                .forEach(skippedNames::add);


        if (namesToActuallyCreate.isEmpty()) {
            logger.info("Bulk Author Creation: No new authors to create. Skipped: {}", skippedNames);
            return new BulkCreationResult<>(Collections.emptyList(), skippedNames);
        }

        // Create new Author entities using streams
        List<Author> newAuthors = namesToActuallyCreate.stream()
                .map(name -> Author.builder().name(name).build())
                .toList(); // Use toList() for immutability

        // Use saveAll for batching
        List<Author> savedAuthors = authorRepository.saveAll(newAuthors);
        logger.info("Bulk Author Creation: Created {} authors. Skipped {} authors.",
                savedAuthors.size(), skippedNames.size());

        // Cache invalidation (optional: add new authors to cache individually if needed)
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        List<String> createdNames = savedAuthors.stream().map(Author::getName).toList();

        return new BulkCreationResult<>(createdNames, skippedNames);
    }


    // --- Other existing methods (findAuthorDtoById, findAllAuthorDtos, etc.) ---
    // These methods remain largely unchanged but benefit from the updated convertToDto
    // and potentially updated cache logic if findByName is cached.

    @Transactional(readOnly = true)
    public Optional<AuthorDto> findAuthorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Author> cachedAuthor = cache.get(cacheKey);
        if (cachedAuthor.isPresent()) {
            // Convert cached entity to DTO *here* to reflect latest related builds
            return Optional.of(convertToDto(cachedAuthor.get()));
        }

        Optional<Author> authorOpt = authorRepository.findById(id);
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
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Author>> cachedResult = cache.get(queryKey);
        List<Author> authors;
        if (cachedResult.isPresent()) {
            authors = cachedResult.get();
        } else {
            authors = authorRepository.fuzzyFindByName(name);
            cache.put(queryKey, authors);
        }
        // Convert to DTOs *after* retrieving from cache/DB
        return authors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Author> findAuthors(String name) {
        // This method returns raw entities, might not be needed externally often
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

        String oldName = author.getName(); // Get old name before changing
        final boolean nameChanged = !oldName.equalsIgnoreCase(newName);

        author.setName(newName);
        Author updatedAuthor = authorRepository.save(author);
        logger.info("Updated Author with ID: {}", updatedAuthor.getId());

        // Update cache for this specific item by ID and potentially name
        String idCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedAuthor.getId());
        cache.put(idCacheKey, updatedAuthor);

        if (nameChanged) {
            String oldNameCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName);
            String newNameCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedAuthor.getName());
            cache.evict(oldNameCacheKey); // Evict old name key
            cache.put(newNameCacheKey, updatedAuthor); // Add new name key
        } else {
            // If name didn't change, still update the name cache entry in case other details did
            String nameCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedAuthor.getName());
            cache.put(nameCacheKey, updatedAuthor);
        }

        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Invalidate lists/queries

        return updatedAuthor;
    }

    // --- deleteAuthorInternal, deleteAuthor, deleteAuthorByName ---
    // Need modification to handle cache eviction for name key as well

    private void deleteAuthorInternal(Author author) {
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        boolean buildCacheInvalidated = false;

        for (Build build : builds) {
            buildCacheInvalidated = true;
            String buildIdCacheKey = InMemoryCache.generateKey(StringConstants.BUILD, build.getId());
            String buildNameCacheKey = InMemoryCache.generateKey(StringConstants.BUILD, build.getName());

            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                logger.warn("Deleting Build ID {} as its last author {} (ID {}) is being deleted.",
                        build.getId(), author.getName(), author.getId());
                cache.evict(buildIdCacheKey);
                cache.evict(buildNameCacheKey); // Evict build name cache
                buildRepository.delete(build);
            } else {
                build.getAuthors().remove(author);
                Build updatedBuild = buildRepository.save(build);
                // Update cache for the modified build
                cache.put(buildIdCacheKey, updatedBuild);
                cache.put(buildNameCacheKey, updatedBuild); // Update build name cache
            }
        }

        Long authorId = author.getId();
        String authorName = author.getName();
        authorRepository.delete(author);
        logger.info("Deleted Author with ID: {}, Name: {}", authorId, authorName);

        // Evict the deleted author from cache
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, authorId));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, authorName)); // Evict name cache
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        // Evict build query caches if builds were affected
        if (buildCacheInvalidated) {
            cache.evictQueryCacheByType(StringConstants.BUILD);
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