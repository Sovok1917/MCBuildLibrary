// file: src/main/java/sovok/mcbuildlibrary/service/ThemeService.java
package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException; // Import
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.dto.ThemeDto;
// Removed import for EntityInUseException
// Removed import for ResourceNotFoundException
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;

@Service
public class ThemeService {

    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.THEME;

    private final ThemeRepository themeRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache;

    public ThemeService(ThemeRepository themeRepository, BuildRepository buildRepository,
                        InMemoryCache cache) {
        this.themeRepository = themeRepository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    private ThemeDto convertToDto(Theme theme) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo
                = buildRepository.findBuildIdAndNameByThemeId(theme.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ThemeDto(theme.getId(), theme.getName(), relatedBuildDtos);
    }

    @Transactional
    public Theme findOrCreateTheme(String name) {
        return themeRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Theme '{}' not found, creating new.", name);
                    Theme newTheme = Theme.builder().name(name).build();
                    return themeRepository.save(newTheme);
                });
    }

    @Transactional
    public Theme createTheme(String name) {
        Optional<Theme> existingTheme = themeRepository.findByName(name);
        if (existingTheme.isPresent()) {
            // Throw IllegalArgumentException for duplicate name conflict
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE,
                    StringConstants.WITH_NAME, name, StringConstants.ALREADY_EXISTS_MESSAGE));
        }
        Theme theme = Theme.builder().name(name).build();
        Theme savedTheme = themeRepository.save(theme);
        logger.info("Created Theme with ID: {}", savedTheme.getId());

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedTheme.getId()), savedTheme);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedTheme;
    }

    @Transactional(readOnly = true)
    public Optional<ThemeDto> findThemeDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Theme> cachedTheme = cache.get(cacheKey);
        if (cachedTheme.isPresent()) {
            return cachedTheme.map(this::convertToDto);
        }

        Optional<Theme> themeOpt = themeRepository.findById(id);
        themeOpt.ifPresent(theme -> cache.put(cacheKey, theme));
        return themeOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findAllThemeDtos() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);
        Optional<List<Theme>> cachedThemes = cache.get(cacheKey);
        if (cachedThemes.isPresent()) {
            return cachedThemes.get().stream().map(this::convertToDto).toList();
        }

        List<Theme> themes = themeRepository.findAll();
        // if (themes.isEmpty()) {
        //     throw new NoSuchElementException(String.format( // Changed from ResourceNotFoundException
        //             StringConstants.NO_ENTITIES_AVAILABLE, StringConstants.THEMES));
        // }
        cache.put(cacheKey, themes);
        return themes.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findThemeDtos(String name) {
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Theme>> cachedResult = cache.get(queryKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get().stream().map(this::convertToDto).toList();
        }

        List<Theme> themes = themeRepository.fuzzyFindByName(name);
        cache.put(queryKey, themes);
        return themes.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Theme> findThemes(String name) {
        Optional<Theme> themeOpt = themeRepository.findByName(name);
        return themeOpt.map(List::of).orElseGet(List::of);
    }

    @Transactional
    public Theme updateTheme(Long id, String newName) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format( // Changed from ResourceNotFoundException
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        Optional<Theme> themeWithSameName = themeRepository.findByName(newName);
        if (themeWithSameName.isPresent() && !themeWithSameName.get().getId().equals(id)) {
            // Throw IllegalArgumentException for duplicate name conflict
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, newName,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        theme.setName(newName);
        Theme updatedTheme = themeRepository.save(theme);
        logger.info("Updated Theme with ID: {}", updatedTheme.getId());

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedTheme.getId()), updatedTheme);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return updatedTheme;
    }

    private void deleteThemeInternal(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            // Throw IllegalStateException for deletion conflict
            throw new IllegalStateException(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                    CACHE_ENTITY_TYPE, theme.getName(), buildsWithTheme.size()));
        }

        Long themeId = theme.getId();
        themeRepository.delete(theme);
        logger.info("Deleted Theme with ID: {}", themeId);

        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, themeId));
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
    }

    @Transactional
    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format( // Changed from ResourceNotFoundException
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }

    @Transactional
    public void deleteThemeByName(String name) {
        Theme theme = themeRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException(String.format( // Changed from ResourceNotFoundException
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }
}