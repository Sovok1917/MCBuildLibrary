// file: src/main/java/sovok/mcbuildlibrary/service/ColorService.java
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
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult; // Import new Result class

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
                    Color savedColor = colorRepository.save(newColor);
                    cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedColor.getId()), savedColor);
                    cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);
                    return savedColor;
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

    /**
     * Creates multiple colors in bulk. Skips names that already exist (case-insensitive).
     *
     * @param namesToCreate A collection of color names to potentially create.
     * @return A BulkCreationResult containing lists of created and skipped names.
     */
    @Transactional
    public BulkCreationResult<String> createColorsBulk(Collection<String> namesToCreate) {
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

        Set<Color> existingColors = colorRepository.findByNamesIgnoreCase(uniqueLowerNames);
        Set<String> existingLowerNames = existingColors.stream()
                .map(Color::getName)
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
            logger.info("Bulk Color Creation: No new colors to create. Skipped: {}", skippedNames);
            return new BulkCreationResult<>(Collections.emptyList(), skippedNames);
        }

        List<Color> newColors = namesToActuallyCreate.stream()
                .map(name -> Color.builder().name(name).build())
                .toList();

        List<Color> savedColors = colorRepository.saveAll(newColors);
        logger.info("Bulk Color Creation: Created {} colors. Skipped {} colors.",
                savedColors.size(), skippedNames.size());

        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        List<String> createdNames = savedColors.stream().map(Color::getName).toList();
        return new BulkCreationResult<>(createdNames, skippedNames);
    }

    @Transactional(readOnly = true)
    public Optional<ColorDto> findColorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);
        Optional<Color> cachedColor = cache.get(cacheKey);
        if (cachedColor.isPresent()) {
            return Optional.of(convertToDto(cachedColor.get()));
        }

        Optional<Color> colorOpt = colorRepository.findById(id);
        colorOpt.ifPresent(color -> cache.put(cacheKey, color));
        return colorOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findAllColorDtos() {
        logger.debug("Fetching all colors from repository (getAll cache disabled).");
        List<Color> colors = colorRepository.findAll();
        return colors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findColorDtos(String name) {
        Map<String, Object> params = Map.of("name", name);
        String queryKey = InMemoryCache.generateQueryKey(CACHE_ENTITY_TYPE, params);

        Optional<List<Color>> cachedResult = cache.get(queryKey);
        List<Color> colors;
        if (cachedResult.isPresent()) {
            colors = cachedResult.get();
        } else {
            colors = colorRepository.fuzzyFindByName(name);
            cache.put(queryKey, colors);
        }
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

        String oldName = color.getName();
        final boolean nameChanged = !oldName.equalsIgnoreCase(newName);

        color.setName(newName);
        Color updatedColor = colorRepository.save(color);
        logger.info("Updated Color with ID: {}", updatedColor.getId());

        // Update cache
        String idCacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedColor.getId());
        cache.put(idCacheKey, updatedColor);
        if (nameChanged) {
            cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, oldName));
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedColor.getName()), updatedColor);
        } else {
            cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedColor.getName()), updatedColor);
        }
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
        String colorName = color.getName();
        colorRepository.delete(color);
        logger.info("Deleted Color with ID: {}, Name: {}", colorId, colorName);

        // Evict cache
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, colorId));
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, colorName));
        cache.evictQueryCacheByType(CACHE_ENTITY_TYPE);

        // Invalidate build query cache
        cache.evictQueryCacheByType(StringConstants.BUILD);
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