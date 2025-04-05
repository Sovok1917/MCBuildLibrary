// file: src/main/java/sovok/mcbuildlibrary/service/ColorService.java
package sovok.mcbuildlibrary.service;

import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache; // Import Cache
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.EntityInUseException;
import sovok.mcbuildlibrary.exception.ErrorMessages;
import sovok.mcbuildlibrary.exception.ResourceNotFoundException;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;

@Service
public class ColorService {

    private static final Logger logger = LoggerFactory.getLogger(ColorService.class);
    private static final String CACHE_ENTITY_TYPE = ErrorMessages.COLOR;

    private final ColorRepository colorRepository;
    private final BuildRepository buildRepository;
    private final InMemoryCache cache; // Inject Cache

    public ColorService(ColorRepository colorRepository, BuildRepository buildRepository, InMemoryCache cache) {
        this.colorRepository = colorRepository;
        this.buildRepository = buildRepository;
        this.cache = cache;
    }

    // Convert entity to DTO, fetching related build info
    private ColorDto convertToDto(Color color) {
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository.findBuildIdAndNameByColorId(color.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ColorDto(color.getId(), color.getName(), relatedBuildDtos);
    }

    // Used internally by BuildService
    @Transactional
    public Color findOrCreateColor(String name) {
        return colorRepository.findByName(name)
                .orElseGet(() -> {
                    logger.info("Color '{}' not found, creating new.", name);
                    Color newColor = Color.builder().name(name).build();
                    Color savedColor = colorRepository.save(newColor);
                    // Optionally cache here if needed
                    // cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedColor.getId()), savedColor);
                    // cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
                    return savedColor;
                });
    }

    @Transactional
    public Color createColor(String name) {
        Optional<Color> existingColor = colorRepository.findByName(name);
        if (existingColor.isPresent()) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }
        Color color = Color.builder().name(name).build();
        Color savedColor = colorRepository.save(color);
        logger.info("Created Color with ID: {}", savedColor.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, savedColor.getId()), savedColor);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---

        return savedColor;
    }

    @Transactional(readOnly = true)
    public Optional<ColorDto> findColorDtoById(Long id) {
        String cacheKey = InMemoryCache.generateKey(CACHE_ENTITY_TYPE, id);

        // --- Cache Read ---
        Optional<Color> cachedColor = cache.get(cacheKey);
        if (cachedColor.isPresent()) {
            return cachedColor.map(this::convertToDto);
        }
        // --- End Cache Read ---

        Optional<Color> colorOpt = colorRepository.findById(id);
        colorOpt.ifPresent(color -> cache.put(cacheKey, color)); // Cache the entity
        return colorOpt.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findAllColorDtos() {
        String cacheKey = InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE);

        // --- Cache Read ---
        Optional<List<Color>> cachedColors = cache.get(cacheKey);
        if (cachedColors.isPresent()) {
            // Convert cached entities to DTOs
            return cachedColors.get().stream().map(this::convertToDto).toList();
        }
        // --- End Cache Read ---

        List<Color> colors = colorRepository.findAll();
        if (colors.isEmpty()) {
            throw new ResourceNotFoundException(String.format(ErrorMessages.NO_ENTITIES_AVAILABLE, "colors"));
        }

        // --- Cache Write ---
        cache.put(cacheKey, colors); // Cache the list of entities
        // --- End Cache Write ---

        return colors.stream().map(this::convertToDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ColorDto> findColorDtos(String name) {
        // Skipping cache for fuzzy find
        List<Color> colors = colorRepository.fuzzyFindByName(name);
        return colors.stream().map(this::convertToDto).toList();
    }

    // Internal use for update/delete by name
    @Transactional(readOnly = true)
    public List<Color> findColors(String name) {
        // Exact match findByName
        Optional<Color> colorOpt = colorRepository.findByName(name);
        return colorOpt.map(List::of).orElseGet(List::of);
    }


    @Transactional
    public Color updateColor(Long id, String newName) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));

        Optional<Color> colorWithSameName = colorRepository.findByName(newName);
        if (colorWithSameName.isPresent() && !colorWithSameName.get().getId().equals(id)) {
            throw new EntityInUseException(String.format(ErrorMessages.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                    CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, newName, ErrorMessages.ALREADY_EXISTS_MESSAGE));
        }

        color.setName(newName);
        Color updatedColor = colorRepository.save(color);
        logger.info("Updated Color with ID: {}", updatedColor.getId());

        // --- Cache Modification ---
        cache.put(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, updatedColor.getId()), updatedColor);
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---

        return updatedColor;
    }

    @Transactional
    private void deleteColorInternal(Color color) {
        List<Build> buildsWithColor = buildRepository.findBuildsByColorId(color.getId());
        if (!buildsWithColor.isEmpty()) {
            throw new EntityInUseException(String.format(ErrorMessages.CANNOT_DELETE_ASSOCIATED,
                    CACHE_ENTITY_TYPE, color.getName(), buildsWithColor.size()));
        }
        Long colorId = color.getId(); // Get ID before delete
        colorRepository.delete(color);
        logger.info("Deleted Color with ID: {}", colorId);

        // --- Cache Modification ---
        cache.evict(InMemoryCache.generateKey(CACHE_ENTITY_TYPE, colorId));
        cache.evict(InMemoryCache.generateGetAllKey(CACHE_ENTITY_TYPE));
        // --- End Cache Modification ---
    }

    @Transactional
    public void deleteColor(Long id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_ID, id, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteColorInternal(color);
    }

    @Transactional
    public void deleteColorByName(String name) {
        Color color = colorRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(String.format(ErrorMessages.RESOURCE_NOT_FOUND_TEMPLATE,
                        CACHE_ENTITY_TYPE, ErrorMessages.WITH_NAME, name, ErrorMessages.NOT_FOUND_MESSAGE)));
        deleteColorInternal(color);
    }
}