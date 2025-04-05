// file: src/main/java/sovok/mcbuildlibrary/service/ThemeService.java
package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache; // Import Cache
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;

@Service
public class ThemeService {

    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);
    private static final String CACHE_ENTITY_TYPE = ErrorMessages.THEME;

    private final ThemeRepository themeRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache; // Inject Cache

    public ThemeService(ThemeRepository themeRepository, BuildRepository buildRepository, InMemoryCache cache) {
        this.themeRepository = themeRepository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    // Convert entity to DTO, fetching related build info
    private ThemeDto convertToDto(Theme theme) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository.findBuildIdAndNameByThemeId(theme.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ThemeDto(theme.getId(), theme.getName(), relatedBuildDtos);
    }

    // Used internally by BuildService
    @Transactional
    public Theme findOrCreateTheme(String name) {
        return themeRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Theme '{}' not found, creating new.", name);
                    Theme newTheme = Theme.builder().name(name).build();
                    Theme savedTheme = themeRepository.save(newTheme);
                    // Optionally cache here if needed
                    // cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedTheme.getId()), savedTheme);
                    // cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
                    return savedTheme;
                });
    }

    @Transactional
    public Theme createTheme(String name) {
        Optional<Theme> existingTheme = themeRepository.findByName(name);
        if (existingTheme.isPresent()) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }
        Theme theme = Theme.builder().name(name).build();
        Theme savedTheme = themeRepository.save(theme);
        logger.info("Created Theme with ID: {}", savedTheme.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedTheme.getId()), savedTheme);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---

        return savedTheme;
    }

    @Transactional(readOnly = true)
    public Optional<ThemeDto> findThemeDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);

        // --- Cache Read ---
        Optional<Theme> cachedTheme = cache.get(cacheKey);
        if (cachedTheme.isPresent()) {
            return cachedTheme.map(this::convertToDto);
        }
        // --- End Cache Read ---

        Optional<Theme> themeOpt = themeRepository.findById(id);
        themeOpt.ifPresent(theme -> cache.put(cacheKey, theme)); // Cache the entity
        return themeOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findAllThemeDtos() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);

        // --- Cache Read ---
        Optional<List<Theme>> cachedThemes = cache.get(cacheKey);
        if (cachedThemes.isPresent()) {
            // Convert cached entities to DTOs
            return cachedThemes.get().stream().map(this::convertToDto).toList();
        }
        // --- End Cache Read ---

        List<Theme> themes = themeRepository.findAll();
        if (themes.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ErrorMessages.NO_ENTITIES_AVAILABLE, "themes"));
        }

        // --- Cache Write ---
        cache.put(cacheKey, themes); // Cache the list of entities
        // --- End Cache Write ---

        return themes.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ThemeDto> findThemeDtos(String name) {
        // Skipping cache for fuzzy find
        List<Theme> themes = themeRepository.fuzzyFindByName(name);
        return themes.stream().map(this::convertToDto).toList();
    }

    // Internal use for update/delete by name
    @Transactional(readOnly = true)
    public List<Theme> findThemes(String name) {
        // Exact match findByName
        Optional<Theme> themeOpt = themeRepository.findByName(name);
        return themeOpt.map(List::of).orElseGet(List::of);
    }

    @Transactional
    public Theme updateTheme(Long id, String newName) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));

        Optional<Theme> themeWithSameName = themeRepository.findByName(newName);
        if (themeWithSameName.isPresent() && !themeWithSameName.get().getId().equals(id)) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, newName, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }

        theme.setName(newName);
        Theme updatedTheme = themeRepository.save(theme);
        logger.info("Updated Theme with ID: {}", updatedTheme.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedTheme.getId()), updatedTheme);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---

        return updatedTheme;
    }

    @Transactional
    private void deleteThemeInternal(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            throw new EntityInUseException(String.format(ErrorMessages.CANNOT_DELETE_ASSOCIATED,
                    CACHE_ENTITY_TYPE, theme.getName(), buildsWithTheme.size()));
        }
        Long themeId = theme.getId(); // Get ID before delete
        themeRepository.delete(theme);
        logger.info("Deleted Theme with ID: {}", themeId);

        // --- Cache Modification ---
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, themeId));
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---
    }

    @Transactional
    public void deleteTheme(Long id) {
        Theme theme = themeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }

    @Transactional
    public void deleteThemeByName(String name) {
        Theme theme = themeRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteThemeInternal(theme);
    }
}