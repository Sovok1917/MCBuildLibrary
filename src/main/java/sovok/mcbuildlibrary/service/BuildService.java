package sovok.mcbuildlibrary.service;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.BuildRepository;

/**
 * Service layer for managing {@link Build} entities.
 * Handles CRUD operations, querying, and caching for builds.
 */
@Service
public class BuildService {
    
    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.BUILD;
    
    private final BuildRepository buildRepository;
    private final InMemoryCache cache;
    
    private BuildService self; // For self-invocation to ensure transactional proxy behavior
    
    /**
     * Sets the self-injected proxy of this service.
     *
     * @param self The proxied instance of BuildService.
     */
    @Autowired
    @Lazy
    public void setSelf(BuildService self) {
        this.self = self;
    }
    
    /**
     * Constructs the BuildService.
     *
     * @param buildRepository The repository for build data access.
     * @param cache           The cache for storing build data.
     */
    public BuildService(BuildRepository buildRepository, InMemoryCache cache) {
        this.buildRepository = buildRepository;
        this.cache = cache;
    }
    
    /**
     * Creates a new build.
     *
     * @param build The build entity to create.
     * @return The saved build entity.
     * @throws IllegalArgumentException if a build with the same name already exists.
     */
    @Transactional
    public Build createBuild(Build build) {
        Optional<Build> existingBuild = buildRepository.findByName(build.getName());
        if (existingBuild.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, build.getName(),
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }
        
        Build savedBuild = buildRepository.save(build);
        logger.info("Created Build with ID: {}", savedBuild.getId());
        
        // Cache the newly created build by ID and name
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild);
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()), savedBuild);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Invalidate list caches
        
        return savedBuild;
    }
    
    /**
     * Finds a build by its ID, fetching associations.
     * Uses cache if available.
     *
     * @param id The ID of the build.
     * @return An {@link Optional} containing the build if found.
     */
    @Transactional(readOnly = true)
    public Optional<Build> findBuildByIdWithAssociations(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            return cachedBuild;
        }
        
        Optional<Build> buildOpt = buildRepository.findByIdWithAssociations(id);
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        return buildOpt;
    }
    
    /**
     * Finds a build by its name, fetching associations.
     * Uses cache if available.
     *
     * @param name The name of the build.
     * @return An {@link Optional} containing the build if found.
     */
    @Transactional(readOnly = true)
    public Optional<Build> findByNameWithAssociations(String name) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name);
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent() && cachedBuild.get().getName().equals(name)) {
            return cachedBuild;
        } else if (cachedBuild.isPresent()) {
            cache.evict(cacheKey); // Evict if name mismatch (though unlikely with good keying)
        }
        
        Optional<Build> buildOpt = buildRepository.findByNameWithAssociations(name);
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        return buildOpt;
    }
    
    
    /**
     * Retrieves all builds with associations, supporting pagination.
     * Caching for paginated results is generally complex and often handled at a higher level
     * or by specialized caching solutions if performance dictates.
     * For simplicity, this method directly queries the repository.
     *
     * @param pageable Pagination information.
     * @return A {@link Page} of builds.
     */
    @Transactional(readOnly = true)
    public Page<Build> findAllWithAssociations(Pageable pageable) {
        logger.debug("Fetching all builds with associations, page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());
        return buildRepository.findAllWithAssociations(pageable);
    }
    
    /**
     * Filters builds based on criteria with associations, supporting pagination.
     * Caching for paginated and filtered results is complex.
     * This method directly queries the repository.
     *
     * @param author   Optional author name filter.
     * @param name     Optional build name filter.
     * @param theme    Optional theme name filter.
     * @param color    Optional color name filter.
     * @param pageable Pagination information.
     * @return A {@link Page} of filtered builds.
     */
    @Transactional(readOnly = true)
    public Page<Build> filterBuildsWithAssociations(String author, String name, String theme,
                                                    String color, Pageable pageable) {
        logger.debug("Filtering builds with associations. Criteria: author={}, name={}, theme={}, "
                        + "color={}. Page: {}, Size: {}",
                author, name, theme, color, pageable.getPageNumber(), pageable.getPageSize());
        return buildRepository.findFilteredWithAssociations(author, name, theme, color, pageable);
    }
    
    /**
     * Retrieves the schematic file for a build.
     *
     * @param id The ID of the build.
     * @return An {@link Optional} containing the schematic file bytes if present.
     */
    @Transactional(readOnly = true)
    public Optional<byte[]> getSchemFile(Long id) {
        // Use findByIdWithAssociations to ensure build is loaded, then access schemFile
        // The schemFile itself is FetchType.LAZY on the Build entity,
        // so accessing it here within the transaction is fine.
        return self.findBuildByIdWithAssociations(id)
                .map(Build::getSchemFile)
                .filter(schemBytes -> schemBytes.length > 0);
    }
    
    /**
     * Updates an existing build.
     *
     * @param id               The ID of the build to update.
     * @param updatedBuildData The new data for the build.
     * @return The updated build entity.
     * @throws NoSuchElementException   if the build to update is not found.
     * @throws IllegalArgumentException if the new name conflicts with an existing build.
     */
    @Transactional
    public Build updateBuild(Long id, Build updatedBuildData) {
        Build existingBuild = self.findBuildByIdWithAssociations(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        
        // Check for name conflict only if the name has changed
        if (!existingBuild.getName().equalsIgnoreCase(updatedBuildData.getName())) {
            Optional<Build> buildWithSameName = buildRepository.findByName(
                    updatedBuildData.getName());
            if (buildWithSameName.isPresent() && !buildWithSameName.get().getId().equals(id)) {
                throw new IllegalArgumentException(String.format(
                        StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, updatedBuildData.getName(),
                        StringConstants.ALREADY_EXISTS_MESSAGE));
            }
        }
        
        String oldName = existingBuild.getName();
        final boolean nameChanged = !oldName.equalsIgnoreCase(updatedBuildData.getName());
        
        existingBuild.setName(updatedBuildData.getName());
        existingBuild.setAuthors(updatedBuildData.getAuthors());
        existingBuild.setThemes(updatedBuildData.getThemes());
        existingBuild.setDescription(updatedBuildData.getDescription());
        existingBuild.setColors(updatedBuildData.getColors());
        existingBuild.setScreenshots(updatedBuildData.getScreenshots());
        if (updatedBuildData.getSchemFile() != null
                && updatedBuildData.getSchemFile().length > 0) {
            existingBuild.setSchemFile(updatedBuildData.getSchemFile());
        }
        
        Build savedBuild = buildRepository.save(existingBuild);
        logger.info("Updated Build with ID: {}", savedBuild.getId());
        
        // Update cache
        String newIdKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId());
        String newNameKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName());
        
        cache.put(newIdKey, savedBuild); // Update/put by ID
        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName)); // Evict old name
        }
        cache.put(newNameKey, savedBuild); // Update/put by new name
        
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Invalidate list caches
        
        return savedBuild;
    }
    
    /**
     * Deletes a build by its ID.
     *
     * @param id The ID of the build to delete.
     * @throws NoSuchElementException if the build to delete is not found.
     */
    @Transactional
    public void deleteBuild(Long id) {
        Build build = self.findBuildByIdWithAssociations(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        
        final String name = build.getName();
        buildRepository.deleteById(id);
        logger.info("Deleted Build with ID: {}", id);
        
        // Evict from cache
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Invalidate list caches
    }
    
    /**
     * Finds a build by its identifier (ID or name), fetching associations.
     *
     * @param identifier The ID or exact name of the build.
     * @return The found build entity.
     * @throws NoSuchElementException if the build is not found.
     */
    @Transactional(readOnly = true)
    public Build findBuildByIdentifierWithAssociations(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            return self.findBuildByIdWithAssociations(buildId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    CACHE_ENTITY_TYPE, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            return self.findByNameWithAssociations(identifier)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
    
    /**
     * Finds a build by its identifier (ID or name) without eager fetching associations.
     * This is a simpler version for cases where associations are not immediately needed.
     *
     * @param identifier The ID or exact name of the build.
     * @return The found build entity.
     * @throws NoSuchElementException if the build is not found.
     */
    @Transactional(readOnly = true)
    public Build findBuildByIdentifier(String identifier) {
        try {
            Long buildId = Long.valueOf(identifier);
            // Assuming findById from JpaRepository is sufficient if associations are not needed
            // or will be fetched lazily.
            return buildRepository.findById(buildId)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    CACHE_ENTITY_TYPE, StringConstants.WITH_ID, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        } catch (NumberFormatException e) {
            return buildRepository.findByName(identifier)
                    .orElseThrow(() -> new NoSuchElementException(
                            String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, identifier,
                                    StringConstants.NOT_FOUND_MESSAGE)));
        }
    }
    
    /**
     * Finds a build by ID, eagerly loading associations specifically for log generation.
     *
     * @param id The ID of the build.
     * @return An {@link Optional} containing the build if found.
     */
    @Transactional(readOnly = true)
    public Optional<Build> findBuildFullyLoadedForLog(Long id) {
        logger.debug("Fetching Build with ID {} and eagerly loading associations for logging.", id);
        return buildRepository.findByIdWithAssociationsForLog(id);
    }
    
    /**
     * Finds builds related to a specific entity (Author, Theme, Color) by its ID,
     * with associations eagerly fetched and pagination.
     *
     * @param entityType The type of the entity ("author", "theme", "color").
     * @param entityId   The ID of the entity.
     * @param pageable   Pagination information.
     * @return A {@link Page} of related builds.
     * @throws IllegalArgumentException if the entityType is invalid.
     */
    @Transactional(readOnly = true)
    public Page<Build> findBuildsByRelatedEntityWithAssociations(String entityType, Long entityId,
                                                                 Pageable pageable) {
        logger.debug("Fetching builds related to {} with ID: {}, page: {}, size: {}",
                entityType, entityId, pageable.getPageNumber(), pageable.getPageSize());
        return switch (entityType.toLowerCase()) {
            case "author" ->
                    buildRepository.findBuildsByAuthorIdWithAssociations(entityId, pageable);
            case "theme" ->
                    buildRepository.findBuildsByThemeIdWithAssociations(entityId, pageable);
            case "color" ->
                    buildRepository.findBuildsByColorIdWithAssociations(entityId, pageable);
            default -> throw new IllegalArgumentException("Invalid entity type for related build "
                    + "search: " + entityType);
        };
    }
}