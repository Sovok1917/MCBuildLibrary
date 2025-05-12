package sovok.mcbuildlibrary.service;

import java.util.Collections; // Added
import java.util.List;
import java.util.Map; // Added
import java.util.Set; // Added
import java.util.stream.Collectors; // Added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto; // Added
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;


@Service
public class ColorService extends BaseNamedEntityService<Color, ColorDto, ColorRepository> {
    
    private static final Logger logger = LoggerFactory.getLogger(ColorService.class);
    
    @Autowired
    public ColorService(ColorRepository colorRepository, BuildRepository buildRepository,
                        InMemoryCache cache) {
        super(colorRepository, buildRepository, cache);
    }
    
    @Override
    public ColorDto convertToDto(Color color) {
        if (color == null) {
            return null;
        }
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByColorId(color.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ColorDto(color.getId(), color.getName(), relatedBuildDtos);
    }
    
    @Override
    protected ColorDto convertToDtoWithRelatedBuilds(
            Color color, Map<Long, List<RelatedBuildDto>> relatedBuildsMap) {
        if (color == null) {
            return null;
        }
        List<RelatedBuildDto> relatedBuilds = relatedBuildsMap.getOrDefault(color.getId(),
                Collections.emptyList());
        return new ColorDto(color.getId(), color.getName(), relatedBuilds);
    }
    
    @Override
    protected Map<Long, List<RelatedBuildDto>> fetchRelatedBuildsInBulk(Set<Long> colorIds) {
        if (colorIds == null || colorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BuildRepository.RelatedBuildWithParentId> relations =
                buildRepository.findBuildsByColorIds(colorIds);
        
        return relations.stream()
                .collect(Collectors.groupingBy(
                        BuildRepository.RelatedBuildWithParentId::getParentId,
                        Collectors.mapping(
                                relation -> new RelatedBuildDto(relation.getBuildId(),
                                        relation.getBuildName()),
                                Collectors.toList())
                ));
    }
    
    @Override
    protected String getEntityTypeString() {
        return StringConstants.COLOR;
    }
    
    @Override
    protected String getEntityTypePluralString() {
        return StringConstants.COLORS;
    }
    
    @Override
    protected List<Color> fuzzyFindEntitiesByName(String name) {
        return repository.fuzzyFindByName(name);
    }
    
    @Override
    protected void checkDeletionConstraints(Color color) {
        List<Build> buildsWithColor = buildRepository.findBuildsByColorId(color.getId());
        if (!buildsWithColor.isEmpty()) {
            logger.warn("Attempted to delete Color '{}' (ID: {}) which is associated with {} "
                            + "build(s).",
                    color.getName(), color.getId(), buildsWithColor.size());
            throw new IllegalStateException(
                    String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                            getEntityTypeString(), color.getName(), buildsWithColor.size()));
        }
        logger.debug("No associated builds found for Color '{}' (ID: {}). Deletion allowed.",
                color.getName(), color.getId());
    }
    
    @Override
    protected Color instantiateEntity(String name) {
        return Color.builder().name(name).build();
    }
}