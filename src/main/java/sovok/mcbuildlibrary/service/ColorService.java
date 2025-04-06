package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;

@Service
public class ColorService {

    private static final Logger logger = LoggerFactory.getLogger(ColorService.class);
    private static final String CACHE_ENTITY_TYPE = StringConstants.COLOR;

    private final ColorRepository colorRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache;


    public ColorService(ColorRepository colorRepository, BuildRepository buildRepository,
                        InMemoryCache cache) {
        this.colorRepository = colorRepository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    private ColorDto convertToDto(Color color) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo
                = buildRepository.findBuildIdAndNameByColorId(color.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(),
                        info.getName()))
                .toList();
        return new ColorDto(color.getId(), color.getName(), relatedBuildDtos);
    }

    @Transactional
    public Color findOrCreateColor(String name) {
        return colorRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Color '{}' not found, creating new.", name);
                    Color newColor = Color.builder().name(name).build();
                    return colorRepository.save(newColor);
                });
    }

    @Transactional
    public Color createColor(String name) {
        Optional<Color> existingColor = colorRepository.findByName(name);
        if (existingColor.isPresent()) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }
        Color color = Color.builder().name(name).build();
        Color savedColor = colorRepository.save(color);
        logger.info("Created Color with ID: {}", savedColor.getId());

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedColor.getId()), savedColor);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return savedColor;
    }

    @Transactional(readOnly = true)
    public Optional<ColorDto> findColorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Color> cachedColor = cache.get(cacheKey);
        if (cachedColor.isPresent()) {
            return cachedColor.map(this::convertToDto);
        }

        Optional<Color> colorOpt = colorRepository.findById(id);
        colorOpt.ifPresent(color -> cache.put(cacheKey, color)); // Cache individual item
        return colorOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findAllColorDtos() {
        // REMOVED: Cache check and put for getAll
        logger.debug("Fetching all colors from repository (getAll cache disabled).");
        List<Color> colors = colorRepository.findAll();
        return colors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findColorDtos(String name) {
        // Query caching remains
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);
        Optional<List<Color>> cachedResult = cache.get(queryKey);
        if (cachedResult.isPresent()) {
            return cachedResult.get().stream().map(this::convertToDto).toList();
        }

        List<Color> colors = colorRepository.fuzzyFindByName(name);
        cache.put(queryKey, colors);
        return colors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<Color> findColors(String name) {
        Optional<Color> colorOpt = colorRepository.findByName(name);
        return colorOpt.map(List::of).orElseGet(List::of);
    }


    @Transactional
    public Color updateColor(Long id, String newName) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format(StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                                CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                                StringConstants.NOT_FOUND_MESSAGE)));

        Optional<Color> colorWithSameName = colorRepository.findByName(newName);
        if (colorWithSameName.isPresent() && !colorWithSameName.get().getId().equals(id)) {
            throw new IllegalArgumentException(String.format(
                    StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, newName,
                    StringConstants.ALREADY_EXISTS_MESSAGE));
        }

        color.setName(newName);
        Color updatedColor = colorRepository.save(color);
        logger.info("Updated Color with ID: {}", updatedColor.getId());

        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedColor.getId()), updatedColor);
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        return updatedColor;
    }

    private void deleteColorInternal(Color color) {
        List<Build> buildsWithColor = buildRepository.findBuildsByColorId(color.getId());
        if (!buildsWithColor.isEmpty()) {
            throw new IllegalStateException(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                    CACHE_ENTITY_TYPE, color.getName(), buildsWithColor.size()));
        }

        Long colorId = color.getId();
        colorRepository.delete(color);
        logger.info("Deleted Color with ID: {}", colorId);

        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, colorId));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
    }

    @Transactional
    public void deleteColor(Long id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_ID, id,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteColorInternal(color);
    }

    @Transactional
    public void deleteColorByName(String name) {
        Color color = colorRepository.findByName(name)
                .orElseThrow(() -> new NoSuchElementException(String.format(
                        StringConstants.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, StringConstants.WITH_NAME, name,
                        StringConstants.NOT_FOUND_MESSAGE)));
        deleteColorInternal(color);
    }
}