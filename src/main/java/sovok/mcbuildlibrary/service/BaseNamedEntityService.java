// file: src/main/java/sovok/mcbuildlibrary/service/BaseNamedEntityService.java
package sovok.mcbuildlibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.BaseNamedEntity;
import sovok.mcbuildlibrary.repository.BaseNamedEntityRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Abstract base service for entities extending BaseNamedEntity.
 * Provides common CRUD operations, caching, and bulk creation logic.
 *
 * @param <T>   The specific entity type (e.g., Author, Theme, Color).
 * @param <DTO> The DTO type for this entity (e.g., AuthorDto).
 * @param <R>   The specific repository type (e.g., AuthorRepository).
 */
public abstract class BaseNamedEntityService<
        T extends BaseNamedEntity,
        DTO, // Generic DTO type
        R extends BaseNamedEntityRepository<T>> { // Generic Repository type

    private static final Logger logger = LoggerFactory.getLogger(BaseNamedEntityService.class);

    protected final R repository;
    protected final BuildRepository buildRepository;
    protected final InMemoryCache cache;

    protected BaseNamedEntityService(R repository, BuildRepository buildRepository, InMemoryCache cache) {
        this.repository = repository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    // --- Abstract Methods (Must be implemented by subclasses) ---

    /**
     * Converts the entity to its corresponding DTO.
     * This typically involves fetching related data (like builds).
     * Made public so it can be called from the controller layer helpers.
     *
     * @param entity The entity to convert.
     * @return The created DTO.
     */
    public abstract DTO convertToDto(T entity); // *** FIX: Changed from protected to public ***

    /**
     * Gets the entity type string used for cache keys and error messages.
     * e.g., "Author", "Theme", "Color".
     *
     * @return The entity type string.
     */
    protected abstract String getEntityTypeString();

    /**
     * Gets the plural entity type string used for error messages.
     * e.g., "Authors", "Themes", "Colors".
     *
     * @return The plural entity type string.
     */
    protected abstract String getEntityTypePluralString();


    /**
     * Performs a fuzzy search for entities by name using the specific repository's implementation.
     *
     * @param name The name query (can be null).
     * @return A list of matching entities.
     */
    protected abstract List<T> fuzzyFindEntitiesByName(String name);

    /**
     * Checks if the entity can be deleted (e.g., checks for associations with Builds).
     * Throws an exception (e.g., IllegalStateException) if deletion is not allowed.
     * Called *before* the entity is actually deleted from the repository.
     *
     * @param entity The entity to check.
     */
    protected abstract void checkDeletionConstraints(T entity);

    /**
     * Abstract factory method to create a new instance of the entity type T.
     * Must be implemented by concrete subclasses.
     * @param name The name for the new entity.
     * @return A new, unsaved instance of T.
     */
    protected abstract T instantiateEntity(String name);


    // --- Concrete Methods (Common Logic - unchanged from previous version) ---
    // ... (create, findById, findByName, findDtoById, findAllDtos, findDtosByNameQuery,
    //      findByFuzzyName, update, deleteById, deleteByName, deleteInternal,
    //      findOrCreate, createBulk, cache helpers etc.) ...

    protected Supplier<NoSuchElementException> notFoundByIdException(Long id) {
        return () -> new NoSuchElementException(
                String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_ID, id, StringConstants.NOT_FOUND_MESSAGE));
    }

    protected Supplier<NoSuchElementException> notFoundByNameException(String name) {
        return () -> new NoSuchElementException(
                String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_NAME, name, StringConstants.NOT_FOUND_MESSAGE));
    }

    protected IllegalArgumentException alreadyExistsException(String name) {
        return new IllegalArgumentException(
                String.format(StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_NAME, name, StringConstants.ALREADY_EXISTS_MESSAGE));
    }


    @Transactional
    public T create(String name) {
        repository.findByName(name).ifPresent(existing -> {
            throw alreadyExistsException(name);
        });
        T entity = instantiateEntity(name);
        T savedEntity = repository.save(entity);
        logger.info("Created {}: {}", getEntityTypeString(), savedEntity);
        cacheEntity(savedEntity);
        evictQueryCaches();
        return savedEntity;
    }

    @Transactional(readOnly = true)
    public Optional<T> findById(Long id) {
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), id);
        Optional<T> cached = cache.get(cacheKey);
        if (cached.isPresent()) {
            return cached;
        }
        Optional<T> entityOpt = repository.findById(id);
        entityOpt.ifPresent(entity -> cache.put(cacheKey, entity));
        return entityOpt;
    }

    @Transactional(readOnly = true)
    public Optional<T> findByName(String name) {
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), name);
        Optional<T> cached = cache.get(cacheKey);
        if(cached.isPresent()) {
            if (cached.get().getName().equals(name)) {
                return cached;
            } else {
                cache.evict(cacheKey);
            }
        }
        Optional<T> entityOpt = repository.findByName(name);
        entityOpt.ifPresent(entity -> cache.put(cacheKey, entity));
        return entityOpt;
    }

    @Transactional(readOnly = true)
    public Optional<DTO> findDtoById(Long id) {
        return findById(id).map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<DTO> findAllDtos() {
        logger.debug("Fetching all {} from repository (getAll cache disabled).", getEntityTypePluralString());
        List<T> entities = repository.findAll();
        return entities.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<DTO> findDtosByNameQuery(String name) {
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, name);
        String queryKey = InMemoryCache.generateQueryKey(getEntityTypeString(), params);
        Optional<List<T>> cachedResult = cache.get(queryKey);
        List<T> entities;
        if (cachedResult.isPresent()) {
            entities = cachedResult.get();
            logger.debug("Cache hit for {} query: {}", getEntityTypeString(), queryKey);
        } else {
            logger.debug("Cache miss for {} query: {}. Fetching from repository.", getEntityTypeString(), queryKey);
            entities = fuzzyFindEntitiesByName(name);
            cache.put(queryKey, entities);
        }
        return entities.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<T> findByFuzzyName(String name) {
        return fuzzyFindEntitiesByName(name);
    }

    @Transactional
    public T update(Long id, String newName) {
        T entity = findById(id).orElseThrow(notFoundByIdException(id));
        repository.findByName(newName).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw alreadyExistsException(newName);
            }
        });
        String oldName = entity.getName();
        final boolean nameChanged = !oldName.equalsIgnoreCase(newName);
        entity.setName(newName);
        T updatedEntity = repository.save(entity);
        logger.info("Updated {}: {}", getEntityTypeString(), updatedEntity);
        if (nameChanged) {
            evictEntityByName(oldName);
        }
        cacheEntity(updatedEntity);
        evictQueryCaches();
        return updatedEntity;
    }

    @Transactional
    public void deleteById(Long id) {
        T entity = findById(id).orElseThrow(notFoundByIdException(id));
        deleteInternal(entity);
    }

    @Transactional
    public void deleteByName(String name) {
        T entity = findByName(name).orElseThrow(notFoundByNameException(name));
        deleteInternal(entity);
    }

    @Transactional
    protected void deleteInternal(T entity) {
        checkDeletionConstraints(entity);
        Long entityId = entity.getId();
        String entityName = entity.getName();
        repository.delete(entity);
        logger.info("Deleted {} with ID: {}, Name: {}", getEntityTypeString(), entityId, entityName);
        evictEntityById(entityId);
        evictEntityByName(entityName);
        evictQueryCaches();
        cache.evictQueryCacheByType(StringConstants.BUILD);
    }

    @Transactional
    public T findOrCreate(String name) {
        return findByName(name)
                .orElseGet(() -> {
                    logger.info("{} '{}' not found, creating new.", getEntityTypeString(), name);
                    return create(name);
                });
    }

    @Transactional
    public BulkCreationResult<String> createBulk(Collection<String> namesToCreate) {
        if (namesToCreate == null || namesToCreate.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), Collections.emptyList());
        }
        Set<String> uniqueLowerNames = namesToCreate.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        if (uniqueLowerNames.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), namesToCreate.stream().toList());
        }
        Set<T> existingEntities = repository.findByNamesIgnoreCase(uniqueLowerNames);
        Set<String> existingLowerNames = existingEntities.stream()
                .map(BaseNamedEntity::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        Map<Boolean, List<String>> partitionedNames = namesToCreate.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .collect(Collectors.partitioningBy(
                        name -> !existingLowerNames.contains(name.toLowerCase())
                ));
        List<String> namesToActuallyCreate = partitionedNames.get(true);
        List<String> skippedNames = partitionedNames.get(false);
        namesToCreate.stream()
                .filter(name -> name == null || name.isBlank())
                .forEach(skippedNames::add);
        if (namesToActuallyCreate.isEmpty()) {
            logger.info("Bulk {} Creation: No new entities to create. Skipped: {}", getEntityTypePluralString(), skippedNames);
            return new BulkCreationResult<>(Collections.emptyList(), skippedNames);
        }
        List<T> newEntities = namesToActuallyCreate.stream()
                .map(this::instantiateEntity)
                .toList();
        List<T> savedEntities = repository.saveAll(newEntities);
        logger.info("Bulk {} Creation: Created {} entities. Skipped {} entities.",
                getEntityTypePluralString(), savedEntities.size(), skippedNames.size());
        savedEntities.forEach(this::cacheEntity);
        evictQueryCaches();
        List<String> createdNames = savedEntities.stream().map(BaseNamedEntity::getName).toList();
        return new BulkCreationResult<>(createdNames, skippedNames);
    }

    protected void cacheEntity(T entity) {
        if (entity == null || entity.getId() == null || entity.getName() == null) {
            return;
        }
        String entityType = getEntityTypeString();
        cache.put(InMemoryCache.generateKey(entityType, entity.getId()), entity);
        cache.put(InMemoryCache.generateKey(entityType, entity.getName()), entity);
        logger.trace("Cached {} ID: {}, Name: {}", entityType, entity.getId(), entity.getName());
    }

    protected void evictEntityById(Long id) {
        if (id == null) return;
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), id);
        cache.evict(cacheKey);
        logger.trace("Evicted {} from cache by ID: {}", getEntityTypeString(), id);
    }

    protected void evictEntityByName(String name) {
        if (name == null) return;
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), name);
        cache.evict(cacheKey);
        logger.trace("Evicted {} from cache by Name: {}", getEntityTypeString(), name);
    }

    protected void evictQueryCaches() {
        String entityType = getEntityTypeString();
        cache.evictQueryCacheByType(entityType);
        logger.trace("Evicted all query caches for type: {}", entityType);
    }
}