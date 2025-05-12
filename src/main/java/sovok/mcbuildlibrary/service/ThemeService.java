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
import sovok.mcbuildlibrary.dto.RelatedBuildDto; // Added
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;


@Service
public class ThemeService extends BaseNamedEntityService<Theme, ThemeDto, ThemeRepository> {
    
    private static final Logger logger = LoggerFactory.getLogger(ThemeService.class);
    
    @Autowired
    public ThemeService(ThemeRepository themeRepository, BuildRepository buildRepository,
                        InMemoryCache cache) {
        super(themeRepository, buildRepository, cache);
    }
    
    @Override
    public ThemeDto convertToDto(Theme theme) {
        if (theme == null) {
            return null;
        }
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByThemeId(theme.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new ThemeDto(theme.getId(), theme.getName(), relatedBuildDtos);
    }
    
    @Override
    protected ThemeDto convertToDtoWithRelatedBuilds(
            Theme theme, Map<Long, List<RelatedBuildDto>> relatedBuildsMap) {
        if (theme == null) {
            return null;
        }
        List<RelatedBuildDto> relatedBuilds = relatedBuildsMap.getOrDefault(theme.getId(),
                Collections.emptyList());
        return new ThemeDto(theme.getId(), theme.getName(), relatedBuilds);
    }
    
    @Override
    protected Map<Long, List<RelatedBuildDto>> fetchRelatedBuildsInBulk(Set<Long> themeIds) {
        if (themeIds == null || themeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BuildRepository.RelatedBuildWithParentId> relations =
                buildRepository.findBuildsByThemeIds(themeIds);
        
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
        return StringConstants.THEME;
    }
    
    @Override
    protected String getEntityTypePluralString() {
        return StringConstants.THEMES;
    }
    
    @Override
    protected List<Theme> fuzzyFindEntitiesByName(String name) {
        return repository.fuzzyFindByName(name);
    }
    
    @Override
    protected void checkDeletionConstraints(Theme theme) {
        List<Build> buildsWithTheme = buildRepository.findBuildsByThemeId(theme.getId());
        if (!buildsWithTheme.isEmpty()) {
            logger.warn("Attempted to delete Theme '{}' (ID: {}) which "
                            + "is associated with {} build(s).",
                    theme.getName(), theme.getId(), buildsWithTheme.size());
            throw new IllegalStateException(
                    String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                            getEntityTypeString(), theme.getName(), buildsWithTheme.size()));
        }
        logger.debug("No associated builds found for Theme '{}' (ID: {}). Deletion allowed.",
                theme.getName(), theme.getId());
    }
    
    @Override
    protected Theme instantiateEntity(String name) {
        return Theme.builder().name(name).build();
    }
}