package sovok.mcbuildlibrary.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; // Keep Lazy for self-injection
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class BuildService {

    private static final Logger logger = LoggerFactory.getLogger(BuildService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.BUILD;

    private final BuildRepository buildRepository;
    private final InMemoryCache cache;

    // <<< FIX: Re-introduced self-injection mechanism >>>
    private BuildService self;

    // Use setter injection with @Lazy to handle the bean proxy creation order
    @Autowired
    @Lazy
    public void setSelf(BuildService self) {
        this.self = self;
    }

    // Constructor Injection for mandatory dependencies
    public BuildService(BuildRepository buildRepository, InMemoryCache cache) {
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

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

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId()), savedBuild);
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getName()), savedBuild);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedBuild;
    }

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

    @Transactional(readOnly = true)
    public Optional<Build> findByName(String name) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name);
        Optional<Build> cachedBuild = cache.get(cacheKey);
        if (cachedBuild.isPresent()) {
            if (cachedBuild.get().getName().equals(name)) {
                return cachedBuild;
            } else {
                cache.evict(cacheKey);
            }
        }

        Optional<Build> buildOpt = buildRepository.findByName(name);
        buildOpt.ifPresent(build -> cache.put(cacheKey, build));
        return buildOpt;
    }

    @Transactional(readOnly = true)
    public List<Build> findAll() {
        logger.debug("Fetching all builds from repository (getAll cache disabled).");
        return buildRepository.findAll();
    }

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

    @Transactional(readOnly = true)
    public Optional<byte[]> getSchemFile(Long id) {
        // <<< FIX: Call findBuildById via self-injected proxy >>>
        return self.findBuildById(id)
                .map(Build::getSchemFile)
                .filter(schemBytes -> schemBytes.length > 0);
    }

    @Transactional
    public Build updateBuild(Long id, Build updatedBuildData) {
        // <<< FIX: Call findBuildById via self-injected proxy >>>
        Build existingBuild = self.findBuildById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        Optional<Build> buildWithSameName = buildRepository.findByName(updatedBuildData.getName());
        if (buildWithSameName.isPresent() && !buildWithSameName.get().getId().equals(id)) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, updatedBuildData.getName(),
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        String oldName = existingBuild.getName();
        final boolean nameChanged = !oldName.equalsIgnoreCase(updatedBuildData.getName());

        existingBuild.setName(updatedBuildData.getName());
        existingBuild.setAuthors(updatedBuildData.getAuthors());
        existingBuild.setThemes(updatedBuildData.getThemes());
        existingBuild.setDescription(updatedBuildData.getDescription());
        existingBuild.setColors(updatedBuildData.getColors());
        existingBuild.setScreenshots(updatedBuildData.getScreenshots());
        if (updatedBuildData.getSchemFile() != null && updatedBuildData.getSchemFile().length > 0) {
            existingBuild.setSchemFile(updatedBuildData.getSchemFile());
        }

        Build savedBuild = buildRepository.save(existingBuild);
        logger.info("Updated Build with ID: {}", savedBuild.getId());

        String newName = savedBuild.getName();
        String newIdKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedBuild.getId());
        String newNameKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, newName);

        cache.put(newIdKey, savedBuild);

        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName));
            cache.put(newNameKey, savedBuild);
        } else {
            cache.put(newNameKey, savedBuild);
        }

        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedBuild;
    }


    @Transactional
    public void deleteBuild(Long id) {
        // <<< FIX: Call findBuildById via self-injected proxy >>>
        Build build = self.findBuildById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        final String name = build.getName();

        buildRepository.deleteById(id);
        logger.info("Deleted Build with ID: {}", id);

        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, name));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
    }
}