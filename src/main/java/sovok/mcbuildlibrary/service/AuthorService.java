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
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;

@Service
public class AuthorService extends BaseNamedEntityService<Author, AuthorDto, AuthorRepository> {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthorService.class);
    
    @Autowired
    public AuthorService(AuthorRepository authorRepository, BuildRepository buildRepository,
                         InMemoryCache cache) {
        super(authorRepository, buildRepository, cache);
    }
    
    @Override
    public AuthorDto convertToDto(Author author) {
        if (author == null) {
            return null;
        }
        // This is the N+1 query source for single DTO conversion
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByAuthorId(author.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
    }
    
    @Override
    protected AuthorDto convertToDtoWithRelatedBuilds(
            Author author, Map<Long, List<RelatedBuildDto>> relatedBuildsMap) {
        if (author == null) {
            return null;
        }
        List<RelatedBuildDto> relatedBuilds = relatedBuildsMap.getOrDefault(author.getId(),
                Collections.emptyList());
        return new AuthorDto(author.getId(), author.getName(), relatedBuilds);
    }
    
    @Override
    protected Map<Long, List<RelatedBuildDto>> fetchRelatedBuildsInBulk(Set<Long> authorIds) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<BuildRepository.RelatedBuildWithParentId> relations =
                buildRepository.findBuildsByAuthorIds(authorIds);
        
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
        return StringConstants.AUTHOR;
    }
    
    @Override
    protected String getEntityTypePluralString() {
        return StringConstants.AUTHORS;
    }
    
    @Override
    protected List<Author> fuzzyFindEntitiesByName(String name) {
        return repository.fuzzyFindByName(name);
    }
    
    @Override
    protected void checkDeletionConstraints(Author author) {
        logger.debug("Pre-deletion check for Author '{}' (ID: {}): No constraints"
                        + " preventing deletion itself.",
                author.getName(), author.getId());
    }
    
    @Override
    protected Author instantiateEntity(String name) {
        return Author.builder().name(name).build();
    }
    
    @Override
    @Transactional
    public void deleteInternal(Author author) {
        logger.debug("Performing Author-specific pre-deletion steps for author ID: {}",
                author.getId());
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        boolean buildCacheInvalidated = false;
        
        for (Build build : builds) {
            buildCacheInvalidated = true;
            String buildIdCacheKey = InMemoryCache.generateKey(StringConstants.BUILD,
                    build.getId());
            String buildNameCacheKey = InMemoryCache.generateKey(StringConstants.BUILD,
                    build.getName());
            
            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                logger.warn("Deleting Build ID {} ('{}') as its last author {} (ID {}) is being "
                                + "deleted.",
                        build.getId(), build.getName(), author.getName(), author.getId());
                cache.evict(buildIdCacheKey);
                cache.evict(buildNameCacheKey);
                buildRepository.delete(build);
            } else {
                build.getAuthors().remove(author);
                Build updatedBuild = buildRepository.save(build);
                cache.put(buildIdCacheKey, updatedBuild);
                cache.put(buildNameCacheKey, updatedBuild);
                logger.info("Removed author {} (ID {}) from build {} ('{}')",
                        author.getName(), author.getId(), build.getId(), build.getName());
            }
        }
        super.deleteInternal(author); // Calls evictQueryCaches for Author
        if (buildCacheInvalidated) {
            cache.evictQueryCacheByType(StringConstants.BUILD); // Evict build query cache
            logger.debug("Evicted build query cache due to author deletion.");
        }
    }
}