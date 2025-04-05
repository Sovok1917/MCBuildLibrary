// file: src/main/java/sovok/mcbuildlibrary/service/BuildService.java
package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache; // Import Cache
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);
    private static final String CACHE_ENTITY_TYPE = ErrorMessages.BUILD;

    private final BuildRepository buildRepository;
    private final InMemoryCache cache; // Inject Cache

    // Removed direct service dependencies to avoid potential circular issues if services call each other.
    // Build creation/update logic requiring other entities is handled in the controller now.
    public BuildService(BuildRepository buildRepository, InMemoryCache cache) {
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    @Transactional
    public Build createBuild(Build build) {
        // Check for existing build name
        Optional<Build> existingBuild = buildRepository.findByName(build.getName());
        if (existingBuild.isPresent()) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, build.getName(), ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }

        // Save the new build
        Build savedBuild = buildRepository.save(build);
        logger.info("Created Build with ID: {}", savedBuild.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
        // --- End Cache Modification ---

        return savedBuild;
    }

    @Transactional(readOnly = true)
    public Optional<Build> findBuildById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);

        // --- Cache Read ---
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            return cachedBuild;
        }
        // --- End Cache Read ---

        Optional<Build> buildOpt = buildRepository.findById(id);

        // --- Cache Write ---
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        // --- End Cache Write ---

        return buildOpt;
    }

    // Finding by name might return multiple, but logic seems to assume unique name find.
    // Caching exact name matches is possible, but fuzzy/like searches are complex to cache generically.
    @Transactional(readOnly = true)
    public Optional<Build> findByName(String name) {
        // Let's cache specific name lookups if they are intended to be unique identifiers
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name); // Use name as identifier part

        // --- Cache Read ---
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            // Ensure the cached build actually matches the requested name (case-insensitive check perhaps?)
            if(cachedBuild.get().getName().equalsIgnoreCase(name)) {
                return cachedBuild;
            } else {
                // Cache contained a key collision or stale data, evict it.
                cache.evict(cacheKey);
            }
        }
        // --- End Cache Read ---

        // Fetching by name (exact match)
        Optional<Build> buildOpt = buildRepository.findByName(name);

        // --- Cache Write ---
        // Cache only if found, using the exact name as part of the key
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        // --- End Cache Write ---

        return buildOpt;
    }


    @Transactional(readOnly = true)
    public List<Build> findAll() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);

        // --- Cache Read ---
        Optional<List<Build>> cachedBuilds = cache.get(cacheKey);
        if (cachedBuilds.isPresent()) {
            return cachedBuilds.get();
        }
        // --- End Cache Read ---

        List<Build> builds = buildRepository.findAll();
        // Don't throw not found here, let controller handle empty list
        // if (builds.isEmpty()) {
        //     throw new ResourceNotFoundException(String.format(ErrorMessages.NO_ENTITIES_AVAILABLE, "builds"));
        // }

        // --- Cache Write ---
        // Only cache if the list is not empty? Or cache empty lists too? Caching empty list is fine.
        cache.put(cacheKey, builds);
        // --- End Cache Write ---

        return builds;
    }

    @Transactional(readOnly = true)
    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        // Complex queries are hard to cache effectively with a simple key/value store.
        // Skipping cache for filterBuilds. Consider more advanced caching solutions if needed.
        String colorsStr = (colors != null && !colors.isEmpty()) ? String.join(",", colors) : null;
        return buildRepository.fuzzyFilterBuilds(author, name, theme, colorsStr);
    }

    // Screenshot and SchemFile data are derived from the Build entity.
    // Caching the Build entity itself (in findBuildById) is the primary optimization here.
    // These methods don't need separate caching but rely on the cached Build.
    @Transactional(readOnly = true)
    public Optional<String> getScreenshot(Long id, int index) {
        return findBuildById(id) // This uses the cache
                .flatMap(build -> {
                    if (build.getScreenshots() == null || index < 0 || index >= build.getScreenshots().size()) {
                        return Optional.empty();
                    }
                    return Optional.of(build.getScreenshots().get(index));
                });
    }

    @Transactional(readOnly = true)
    public Optional<byte[]> getSchemFile(Long id) {
        return findBuildById(id) // This uses the cache
                .map(Build::getSchemFile) // Get the byte array directly
                .filter(schemBytes -> schemBytes != null && schemBytes.length > 0); // Ensure it's not null/empty
    }


    @Transactional
    public Build updateBuild(Long id, Build updatedBuildData) {
        // Find the existing build
        Build existingBuild = findBuildById(id) // Uses cache
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));

        // Check if the new name conflicts with another existing build
        Optional<Build> buildWithSameName = buildRepository.findByName(updatedBuildData.getName());
        if (buildWithSameName.isPresent() && !buildWithSameName.get().getId().equals(id)) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, updatedBuildData.getName(), ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }

        // Store old name if changed, to evict old name-based cache key if we cached it
        String oldName = existingBuild.getName();
        boolean nameChanged = !oldName.equals(updatedBuildData.getName());

        // Update fields
        existingBuild.setName(updatedBuildData.getName());
        existingBuild.setAuthors(updatedBuildData.getAuthors()); // Assuming these are managed entities
        existingBuild.setThemes(updatedBuildData.getThemes());
        existingBuild.setDescription(updatedBuildData.getDescription());
        existingBuild.setColors(updatedBuildData.getColors());
        existingBuild.setScreenshots(updatedBuildData.getScreenshots());
        if (updatedBuildData.getSchemFile() != null) { // Only update schem if provided
            existingBuild.setSchemFile(updatedBuildData.getSchemFile());
        }

        // Save the updated build
        Build savedBuild = buildRepository.save(existingBuild);
        logger.info("Updated Build with ID: {}", savedBuild.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild); // Update by ID
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache

        // If name was used as a cache key and it changed, evict the old name key and potentially cache the new one
        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName));
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()), savedBuild);
        }
        // --- End Cache Modification ---

        return savedBuild;
    }

    @Transactional
    public void deleteBuild(Long id) {
        // Check existence first to provide a clear error and get data for cache eviction
        Build build = buildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));

        String name = build.getName(); // Get name for potential name-key eviction

        buildRepository.deleteById(id);
        logger.info("Deleted Build with ID: {}", id);

        // --- Cache Modification ---
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id)); // Evict by ID
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name)); // Evict by name (if cached)
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE)); // Invalidate list cache
        // --- End Cache Modification ---
    }
}