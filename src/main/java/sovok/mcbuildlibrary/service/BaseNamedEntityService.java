package sovok.mcbuildlibrary.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.RelatedBuildDto; // Added import
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.BaseNamedEntity;
import sovok.mcbuildlibrary.repository.BaseNamedEntityRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult;


@SuppressWarnings({"java:S6809", "unused"})
public abstract class BaseNamedEntityService<
        T extends BaseNamedEntity,
        D, // D is the DTO type (e.g., AuthorDto)
        R extends BaseNamedEntityRepository<T>> {
    
    private static final Logger logger = LoggerFactory.getLogger(BaseNamedEntityService.class);
    
    protected final R repository;
    protected final BuildRepository buildRepository;
    protected final InMemoryCache cache;
    
    protected BaseNamedEntityService(R repository, BuildRepository buildRepository,
                                     InMemoryCache cache) {
        this.repository = repository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }
    
    /**
     * Converts a single entity to its DTO, including related builds.
     * This version is suitable for fetching a single entity's details.
     *
     * @param entity The entity to convert.
     * @return The DTO representation.
     */
    public abstract D convertToDto(T entity);
    
    /**
     * Converts an entity to its DTO using a pre-fetched map of related builds.
     * This version is optimized for bulk DTO conversion.
     *
     * @param entity The entity to convert.
     * @param relatedBuildsMap A map where the key is the entity ID and the value is a list of
     *                         its {@link RelatedBuildDto}s.
     * @return The DTO representation.
     */
    protected abstract D convertToDtoWithRelatedBuilds(
            T entity, Map<Long, List<RelatedBuildDto>> relatedBuildsMap);
    
    /**
     * Fetches related builds in bulk for a given set of entity IDs.
     *
     * @param entityIds The set of parent entity IDs.
     * @return A map where keys are parent entity IDs and values are lists of {@link
     * RelatedBuildDto}.
     */
    protected abstract Map<Long, List<RelatedBuildDto>> fetchRelatedBuildsInBulk(Set<Long>
                                                                                         entityIds);
    
    
    protected abstract String getEntityTypeString();
    
    protected abstract String getEntityTypePluralString();
    
    protected abstract List<T> fuzzyFindEntitiesByName(String name);
    
    protected abstract void checkDeletionConstraints(T entity);
    
    protected abstract T instantiateEntity(String name);
    
    protected Supplier<NoSuchElementException> notFoundByIdException(Long id) {
        return () -> new NoSuchElementException(
                String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_ID, id, StringConstants
                                .NOT_FOUND_MESSAGE));
    }
    
    protected Supplier<NoSuchElementException> notFoundByNameException(String name) {
        return () -> new NoSuchElementException(
                String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_NAME, name, StringConstants
                                .NOT_FOUND_MESSAGE));
    }
    
    protected IllegalArgumentException alreadyExistsException(String name) {
        return new IllegalArgumentException(
                String.format(StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        getEntityTypeString(), StringConstants.WITH_NAME, name, StringConstants
                                .ALREADY_EXISTS_MESSAGE));
    }
    
    @Transactional
    public T create(String name) {
        repository.findByName(name).ifPresent(existing -> {
            throw alreadyExistsException(name);
        });
        T entity = this.instantiateEntity(name);
        T savedEntity = repository.save(entity);
        logger.info("Created {}: {}", getEntityTypeString(), savedEntity);
        this.cacheEntity(savedEntity);
        this.evictQueryCaches();
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
        if (cached.isPresent()) {
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
    public Optional<D> findDtoById(Long id) {
        return this.findById(id).map(this::convertToDto); // Uses single DTO conversion
    }
    
    @Transactional(readOnly = true)
    public List<D> findAllDtos() {
        logger.debug("Fetching all {} DTOs with optimized related build loading.",
                getEntityTypePluralString());
        List<T> entities = repository.findAll();
        if (entities.isEmpty()) {
            return Collections.emptyList();
        }
        
        Set<Long> entityIds = entities.stream().map(BaseNamedEntity::getId).collect(
                Collectors.toSet());
        Map<Long, List<RelatedBuildDto>> relatedBuildsMap = fetchRelatedBuildsInBulk(entityIds);
        
        return entities.stream()
                .map(entity -> convertToDtoWithRelatedBuilds(entity, relatedBuildsMap))
                .toList();
    }
    
    
    @Transactional(readOnly = true)
    public List<D> findDtosByNameQuery(String name) {
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM,
                name == null ? "__NULL__" : name);
        String queryKey = InMemoryCache.generateQueryKey(getEntityTypeString(), params);
        
        // Attempt to get DTO list directly from cache if previously computed by this method
        Optional<List<D>> cachedDtoList = cache.get(queryKey);
        if (cachedDtoList.isPresent()) {
            logger.debug("Cache hit for {} DTO query: {}", getEntityTypeString(), queryKey);
            return cachedDtoList.get();
        }
        
        logger.debug("Cache miss for {} DTO query: {}. Fetching entities and related builds.",
                getEntityTypeString(), queryKey);
        List<T> entities = this.findByFuzzyName(name); // This might be cached at entity level
        // by Base service
        
        if (entities.isEmpty()) {
            cache.put(queryKey, Collections.emptyList()); // Cache empty DTO list
            return Collections.emptyList();
        }
        
        Set<Long> entityIds = entities.stream().map(BaseNamedEntity::getId).collect(Collectors
                .toSet());
        Map<Long, List<RelatedBuildDto>> relatedBuildsMap = fetchRelatedBuildsInBulk(entityIds);
        
        List<D> dtos = entities.stream()
                .map(entity -> convertToDtoWithRelatedBuilds(entity, relatedBuildsMap))
                .toList();
        
        cache.put(queryKey, dtos); // Cache the computed DTO list
        return dtos;
    }
    
    
    @Transactional(readOnly = true)
    @SuppressWarnings("unused")
    public List<T> findByFuzzyName(String name) {
        return fuzzyFindEntitiesByName(name);
    }
    
    @Transactional
    public T update(Long id, String newName) {
        T entity = this.findById(id).orElseThrow(notFoundByIdException(id));
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
            this.evictEntityByName(oldName);
        }
        this.cacheEntity(updatedEntity);
        this.evictQueryCaches(); // Evict DTO query caches as well
        return updatedEntity;
    }
    
    @Transactional
    public void deleteById(Long id) {
        T entity = this.findById(id).orElseThrow(notFoundByIdException(id));
        this.deleteInternal(entity);
    }
    
    @Transactional
    public void deleteByName(String name) {
        T entity = this.findByName(name).orElseThrow(notFoundByNameException(name));
        this.deleteInternal(entity);
    }
    
    @Transactional
    public void deleteInternal(T entity) {
        this.checkDeletionConstraints(entity);
        Long entityId = entity.getId();
        String entityName = entity.getName();
        repository.delete(entity);
        logger.info("Deleted {} with ID: {}, Name: {}", getEntityTypeString(), entityId,
                entityName);
        this.evictEntityById(entityId);
        this.evictEntityByName(entityName);
        this.evictQueryCaches(); // Evict DTO query caches as well
    }
    
    @Transactional
    public T findOrCreate(String name) {
        return this.findByName(name)
                .orElseGet(() -> {
                    logger.info("{} '{}' not found, creating new.", getEntityTypeString(), name);
                    return this.create(name);
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
        
        List<String> invalidOrBlankNames = namesToCreate.stream()
                .filter(name -> name == null || name.isBlank())
                .toList();
        
        if (uniqueLowerNames.isEmpty()) {
            return new BulkCreationResult<>(Collections.emptyList(), invalidOrBlankNames);
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
                        trimmedName -> !existingLowerNames.contains(trimmedName.toLowerCase())
                ));
        
        List<String> namesToActuallyCreate = partitionedNames.get(true);
        List<String> skippedExistingNames = partitionedNames.get(false);
        
        List<String> allSkippedNames = new ArrayList<>(skippedExistingNames);
        allSkippedNames.addAll(invalidOrBlankNames);
        
        if (namesToActuallyCreate.isEmpty()) {
            logger.info("Bulk {} Creation: No new entities to create. Skipped: {}",
                    getEntityTypePluralString(), allSkippedNames);
            return new BulkCreationResult<>(Collections.emptyList(), allSkippedNames);
        }
        
        List<T> newEntities = namesToActuallyCreate.stream()
                .map(this::instantiateEntity)
                .toList();
        List<T> savedEntities = repository.saveAll(newEntities);
        
        logger.info("Bulk {} Creation: Created {} entities. Skipped {} entities.",
                getEntityTypePluralString(), savedEntities.size(), allSkippedNames.size());
        
        savedEntities.forEach(this::cacheEntity);
        this.evictQueryCaches(); // Evict DTO query caches as well
        
        List<String> createdNames = savedEntities.stream().map(BaseNamedEntity::getName).toList();
        return new BulkCreationResult<>(createdNames, allSkippedNames);
    }
    
    protected void cacheEntity(T entity) {
        if (entity == null || entity.getId() == null || entity.getName() == null) {
            logger.warn("Attempted to cache an invalid entity: {}", entity);
            return;
        }
        String entityType = getEntityTypeString();
        cache.put(InMemoryCache.generateKey(entityType, entity.getId()), entity);
        cache.put(InMemoryCache.generateKey(entityType, entity.getName()), entity);
        logger.trace("Cached {} ID: {}, Name: {}", entityType, entity.getId(), entity.getName());
    }
    
    protected void evictEntityById(Long id) {
        if (id == null) {
            return;
        }
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), id);
        cache.evict(cacheKey);
        logger.trace("Evicted {} from cache by ID: {}", getEntityTypeString(), id);
    }
    
    protected void evictEntityByName(String name) {
        if (name == null) {
            return;
        }
        String cacheKey = InMemoryCache.generateKey(getEntityTypeString(), name);
        cache.evict(cacheKey);
        logger.trace("Evicted {} from cache by Name: {}", getEntityTypeString(), name);
    }
    
    /** Evicts query caches for both entities and DTOs of this type. */
    protected void evictQueryCaches() {
        String entityType = getEntityTypeString();
        cache.evictQueryCacheByType(entityType); // Evicts entity query cache
        // Also evict DTO query cache (assuming DTOs might be cached under the same entity
        // Type string
        // or a derived one. If DTOs use a different cache key prefix, adjust accordingly)
        // For simplicity, if DTO query keys use the same entityType, this is sufficient.
        logger.trace("Evicted all query caches for type: {}", entityType);
    }
}