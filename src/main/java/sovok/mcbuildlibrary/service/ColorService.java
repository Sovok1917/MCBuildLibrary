package sovok.mcbuildlibrary.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;

import java.util.List;

@Service
public class ColorService extends BaseNamedEntityService<Color, ColorDto, ColorRepository> {

    private static final Logger logger = LoggerFactory.getLogger(ColorService.class);

    @Autowired
    public ColorService(ColorRepository colorRepository, BuildRepository buildRepository, InMemoryCache cache) {
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
            logger.warn("Attempted to delete Color '{}' (ID: {}) which is associated with {} build(s).",
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
        // This call now works correctly because Color has @SuperBuilder
        return Color.builder().name(name).build();
    }
}