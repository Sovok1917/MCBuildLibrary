package sovok.mcbuildlibrary.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Import Autowired
import org.springframework.context.annotation.Lazy; // Import Lazy
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.BUILD;

    private final BuildRepository buildRepository;
    private final InMemoryCache cache;

    // --- Self-injection Fix ---
    private BuildService self;

    @Autowired
    @Lazy // Use @Lazy to break potential immediate circular dependency during bean creation
    public void setSelf(BuildService self) {
        this.self = self;
    }
    // --- End Self-injection Fix ---

    public BuildService(BuildRepository buildRepository, InMemoryCache cache) {
        this.buildRepository = buildRepository;
        this.cache = cache;
        // Note: 'self' will be injected via setter after construction
    }

    // ... (createBuild remains the same) ...
    @Transactional
    public Build createBuild(Build build) {
        Optional<Build> existingBuild = buildRepository.findByName(build.getName());
        if (existingBuild.isPresent()) {
            throw new EntityInUseException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, build.getName(),
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        Build savedBuild = buildRepository.save(build);
        logger.info("Created Build with ID: {}", savedBuild.getId());

        // --- Cache Invalidation/Update ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild);
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()), savedBuild);
        // Cache by name too
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE); // Invalidate query caches
        // --- End Cache Invalidation/Update ---

        return savedBuild;
    }

    // Marked readOnly, good practice if called externally
    @Transactional(readOnly = true)
    public Optional<Build> findBuildById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            return cachedBuild;
        }

        Optional<Build> buildOpt = buildRepository.findById(id);
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        return buildOpt;
    }

    // Marked readOnly
    @Transactional(readOnly = true)
    public Optional<Build> findByName(String name) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name); // Key by name
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            if (cachedBuild.get().getName().equalsIgnoreCase(name)) {
                return cachedBuild;
            } else {
                cache.evict(cacheKey);
            }
        }

        Optional<Build> buildOpt = buildRepository.findByName(name);
        buildOpt.ifPresent(build -> cache.put(cacheKey, build)); // Cache by name
        return buildOpt;
    }

    // Marked readOnly
    @Transactional(readOnly = true)
    public List<Build> findAll() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);
        Optional<List<Build>> cachedBuilds = cache.get(cacheKey);
        if (cachedBuilds.isPresent()) {
            return cachedBuilds.get();
        }

        List<Build> builds = buildRepository.findAll();
        cache.put(cacheKey, builds);
        return builds;
    }

    // Marked readOnly
    @Transactional(readOnly = true)
    public List<Build> filterBuilds(String author, String name, String theme, String color) {
        Map<String, Object> params = new HashMap<>();
        params.put("author", author);
        params.put("name", name);
        params.put("theme", theme);
        params.put("color", color);

        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Build>> cachedResult = cache.get(queryKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get();
        }

        List<Build> filteredBuilds = buildRepository.fuzzyFilterBuilds(author, name, theme, color);
        cache.put(queryKey, filteredBuilds);
        return filteredBuilds;
    }

    // This method should be transactional if it needs a consistent view
    // or if findBuildById wasn't transactional itself. readOnly=true is appropriate.
    @Transactional(readOnly = true) // Line 132 approx
    public Optional<String> getScreenshot(Long id, int index) {
        // Call findBuildById via self to ensure transaction propagation if needed
        return self.findBuildById(id) // Line 135: Use self.
                .flatMap(build -> {
                    if (build.getScreenshots() == null || index < 0 || index
                            >= build.getScreenshots().size()) {
                        return Optional.empty();
                    }
                    return Optional.of(build.getScreenshots().get(index));
                });
    }

    // Similar to getScreenshot, add readOnly transaction
    @Transactional(readOnly = true) // Line 143 approx
    public Optional<byte[]> getSchemFile(Long id) {
        // Call findBuildById via self
        return self.findBuildById(id) // Line 146: Use self.
                .map(Build::getSchemFile)
                .filter(schemBytes -> schemBytes.length > 0);
    }

    // This is a write operation, @Transactional is correct
    @Transactional // Line 150 approx
    public Build updateBuild(Long id, Build updatedBuildData) {
        // Call findBuildById via self
        Build existingBuild = self.findBuildById(id) // Line 153: Use self.
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        // Direct repo calls are fine within the already started transaction
        Optional<Build> buildWithSameName = buildRepository.findByName(updatedBuildData.getName());
        if (buildWithSameName.isPresent() && !buildWithSameName.get().getId().equals(id)) {
            throw new EntityInUseException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, updatedBuildData.getName(),
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        String oldName = existingBuild.getName();
        final boolean nameChanged = !oldName.equals(updatedBuildData.getName());

        existingBuild.setName(updatedBuildData.getName());
        existingBuild.setAuthors(updatedBuildData.getAuthors());
        existingBuild.setThemes(updatedBuildData.getThemes());
        existingBuild.setDescription(updatedBuildData.getDescription());
        existingBuild.setColors(updatedBuildData.getColors());
        existingBuild.setScreenshots(updatedBuildData.getScreenshots());
        if (updatedBuildData.getSchemFile() != null) {
            existingBuild.setSchemFile(updatedBuildData.getSchemFile());
        }


        Build savedBuild = buildRepository.save(existingBuild);
        logger.info("Updated Build with ID: {}", savedBuild.getId());

        // Cache updates...
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName));
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()),
                    savedBuild);
        } else {
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()),
                    savedBuild);
        }

        return savedBuild;
    }

    @Transactional
    public void deleteBuild(Long id) {
        // Direct repo call is fine within this transaction
        Build build = buildRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        final String name = build.getName();

        buildRepository.deleteById(id);
        logger.info("Deleted Build with ID: {}", id);

        // Cache invalidation...
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name));
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
    }
}