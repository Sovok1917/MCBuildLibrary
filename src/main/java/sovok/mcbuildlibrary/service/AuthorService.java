package sovok.mcbuildlibrary.service;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Added
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
        List<BuildRepository.BuildIdAndName> relatedBuildsInfo = buildRepository
                .findBuildIdAndNameByAuthorId(author.getId());
        List<RelatedBuildDto> relatedBuildDtos = relatedBuildsInfo.stream()
                .map(info -> new RelatedBuildDto(info.getId(), info.getName()))
                .toList();
        return new AuthorDto(author.getId(), author.getName(), relatedBuildDtos);
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
        // Author deletion *is* allowed, but triggers build handling logic.
        // So, no exception needs to be thrown here. The actual handling
        // is done in the overridden deleteInternal method.
        logger.debug("Pre-deletion check for Author '{}' (ID: {}): No constraints"
                        + " preventing deletion itself.",
                author.getName(), author.getId());
    }

    @Override
    protected Author instantiateEntity(String name) {
        // Correctly uses the Lombok builder for the Author class
        return Author.builder().name(name).build(); // *** FIX: Verified this is correct ***
    }

    // Override deleteInternal to handle build associations BEFORE deleting the author
    @Override // *** FIX: This override is now valid because base method is protected ***
    @Transactional // Ensure build modifications/deletions are in the same transaction
    public void deleteInternal(Author author) {
        logger.debug("Performing Author-specific pre-deletion steps for author ID: {}",
                author.getId());
        List<Build> builds = buildRepository.findBuildsByAuthorId(author.getId());
        boolean buildCacheInvalidated = false;

        for (Build build : builds) {
            buildCacheInvalidated = true; // Mark that build cache needs invalidation
            String buildIdCacheKey = InMemoryCache.generateKey(StringConstants.BUILD,
                    build.getId());
            String buildNameCacheKey = InMemoryCache.generateKey(StringConstants.BUILD,
                    build.getName());

            if (build.getAuthors().size() == 1 && build.getAuthors().contains(author)) {
                // This is the last author, delete the build
                logger.warn("Deleting Build ID {} ('{}') as its last author {} (ID {}) is being "
                                + "deleted.",
                        build.getId(), build.getName(), author.getName(), author.getId());
                cache.evict(buildIdCacheKey);
                cache.evict(buildNameCacheKey);
                buildRepository.delete(build); // Delete the build
            } else {
                // Remove the author from the build's author list and save
                build.getAuthors().remove(author);
                Build updatedBuild = buildRepository.save(build);
                // Update build cache
                cache.put(buildIdCacheKey, updatedBuild);
                cache.put(buildNameCacheKey, updatedBuild);
                logger.info("Removed author {} (ID {}) from build {} ('{}')",
                        author.getName(), author.getId(), build.getId(), build.getName());
            }
        }

        // Now call the base class deleteInternal to delete the author entity and handle author
        // cache eviction
        super.deleteInternal(author); // *** FIX: This call is now valid ***

        // Evict build query caches if any builds were affected
        if (buildCacheInvalidated) {
            cache.evictQueryCacheByType(StringConstants.BUILD);
            logger.debug("Evicted build query cache due to author deletion.");
        }
    }

}