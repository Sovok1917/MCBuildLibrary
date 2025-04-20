// file: src/main/java/sovok/mcbuildlibrary/service/ThemeService.java
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
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult; // Import new Result class


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
                    Theme savedTheme = themeRepository.save(newTheme);
                    cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedTheme.getId()), savedTheme);
                    cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
                    return savedTheme;
                });
    }

    @Transactional
    public Theme createTheme(String name) {
        Optional<Theme> existingTheme = themeRepository.findByName(name);
        if (existingTheme.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE,
                    StringConstants.WITH_NAME, name, StringConstants.ALREADY_EXISTS_MESSAGE));
        }
        Theme theme = Theme.builder().name(name).build();
        Theme savedTheme = themeRepository.save(theme);
        logger.info("Created Theme with ID: {}", savedTheme.getId());

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedTheme.getId()), savedTheme);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedTheme;
    }

    /**
     * Creates multiple themes in bulk. Skips names that already exist (case-insensitive).
     *
     * @param namesToCreate A collection of theme names to potentially create.
     * @return A BulkCreationResult containing lists of created and skipped names.
     */
    @Transactional
    public BulkCreationResult<String> createThemesBulk(Collection<String> namesToCreate) {
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

        Set<Theme> existingThemes = themeRepository.findByNamesIgnoreCase(uniqueLowerNames);
        Set<String> existingLowerNames = existingThemes.stream()
                .map(Theme::getName)
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
            logger.info("Bulk Theme Creation: No new themes to create. Skipped: {}", skippedNames);
            return new BulkCreationResult<>(Collections.emptyList(), skippedNames);
        }

        List<Theme> newThemes = namesToActuallyCreate.stream()
                .map(name -> Theme.builder().name(name).build())
                .toList();

        List<Theme> savedThemes = themeRepository.saveAll(newThemes);
        logger.info("Bulk Theme Creation: Created {} themes. Skipped {} themes.",
                savedThemes.size(), skippedNames.size());

        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        List<String> createdNames = savedThemes.stream().map(Theme::getName).toList();
        return new BulkCreationResult<>(createdNames, skippedNames);
    }


    @Transactional(readOnly = true)
    public Optional<ThemeDto> findThemeDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Theme> cachedTheme = cache.get(cacheKey);
        if (cachedTheme.isPresent()) {
            return Optional.of(convertToDto(cachedTheme.get()));
        }

        Optional<Theme> themeOpt = themeRepository.findById(id);
        themeOpt.ifPresent(theme -> cache.put(cacheKey, theme));
        return themeOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findAllThemeDtos() {
        logger.debug("Fetching all themes from repository (getAll cache disabled).");
        List<Theme> themes = themeRepository.findAll();
        return themes.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findThemeDtos(String name) {
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Theme>> cachedResult = cache.get(queryKey);
        List<Theme> themes;
        if (cachedResult.isPresent()) {
            themes = cachedResult.get();
        } else {
            themes = themeRepository.fuzzyFindByName(name);
            cache.put(queryKey, themes);
        }
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
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));

        Optional<Theme> themeWithSameName = themeRepository.findByName(newName);
        if (themeWithSameName.isPresent() && !themeWithSameName.get().getId().equals(id)) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, newName,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        String oldName = theme.getName();
        final boolean nameChanged = !oldName.equalsIgnoreCase(newName);

        theme.setName(newName);
        Theme updatedTheme = themeRepository.save(theme);
        logger.info("Updated Theme with ID: {}", updatedTheme.getId());

        // Update cache
        String idCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedTheme.getId());
        cache.put(idCacheKey, updatedTheme);
        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName));
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedTheme.getName()), updatedTheme);
        } else {
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedTheme.getName()), updatedTheme);
        }
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return updatedTheme;
    }

    private void deleteThemeInternal(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            throw new IllegalStateException(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                    CACHE_ENTITY_TYPE, theme.getName(), buildsWithTheme.size()));
        }

        Long themeId = theme.getId();
        String themeName = theme.getName();
        themeRepository.delete(theme);
        logger.info("Deleted Theme with ID: {}, Name: {}", themeId, themeName);

        // Evict cache
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, themeId));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, themeName));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        // No direct build modification needed, but associated builds *could* be re-queried,
        // so Build query cache invalidation is reasonable if strict consistency is desired.
        cache.evictQueryCacheByType(StringConstants.BUILD);
    }

    @Transactional
    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }

    @Transactional
    public void deleteThemeByName(String name) {
        Theme theme = themeRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }
}