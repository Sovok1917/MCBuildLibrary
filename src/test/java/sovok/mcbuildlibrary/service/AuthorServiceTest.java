package sovok.mcbuildlibrary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.AuthorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.repository.AuthorRepository;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sovok.mcbuildlibrary.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private AuthorRepository authorRepository;
    @Mock
    private BuildRepository buildRepository;
    @Mock
    private InMemoryCache cache;

    @InjectMocks
    private AuthorService authorService;

    @Captor
    private ArgumentCaptor<Author> authorCaptor;
    @Captor
    private ArgumentCaptor<Build> buildCaptor;
    @Captor
    private ArgumentCaptor<List<Author>> authorListCaptor;

    private Author author1;
    private Author author2;
    private Build build1;
    private Build build2OnlyAuthor1;

    @BeforeEach
    void setUp() {
        author1 = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);
        author2 = createTestAuthor(TEST_ID_2, AUTHOR_NAME_2);

        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, new HashSet<>(Set.of(author1, author2)), Set.of(), Set.of());
        build2OnlyAuthor1 = createTestBuild(TEST_ID_2, BUILD_NAME_2, new HashSet<>(Set.of(author1)), Set.of(), Set.of());
    }

    // --- Test Cases for BaseNamedEntityService Logic ---

    @Test
    @DisplayName("create_whenNameDoesNotExist_shouldSaveAndCacheAuthor")
    void create_whenNameDoesNotExist_shouldSaveAndCacheAuthor() {
        // Arrange
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(author1); // Assume save
        // assigns ID 1

        // Act
        Author createdAuthor = authorService.create(AUTHOR_NAME_1);

        // Assert
        assertThat(createdAuthor).isNotNull();
        assertThat(createdAuthor.getId()).isEqualTo(TEST_ID_1);
        assertThat(createdAuthor.getName()).isEqualTo(AUTHOR_NAME_1);

        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(authorRepository).save(authorCaptor.capture());
        assertThat(authorCaptor.getValue().getName()).isEqualTo(AUTHOR_NAME_1);
        assertThat(authorCaptor.getValue().getId()).isNull(); // Should be null before save

        verify(cache).put(AUTHOR_CACHE_KEY_ID_1, createdAuthor);
        verify(cache).put(AUTHOR_CACHE_KEY_NAME_1, createdAuthor);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
        verify(cache, never()).evictQueryCacheByType(StringConstants.BUILD); // Ensure build cache
        // not evicted on simple create
    }

    @Test
    @DisplayName("create_whenNameExists_shouldThrowIllegalArgumentException")
    void create_whenNameExists_shouldThrowIllegalArgumentException() {
        // Arrange
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));

        // Act & Assert
        assertThatThrownBy(() -> authorService.create(AUTHOR_NAME_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format(StringConstants
                                .RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        StringConstants.AUTHOR, StringConstants.WITH_NAME, AUTHOR_NAME_1,
                        StringConstants.ALREADY_EXISTS_MESSAGE));

        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(authorRepository, never()).save(any());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("findById_whenCached_shouldReturnCachedAuthor")
    void findById_whenCached_shouldReturnCachedAuthor() {
        // Arrange
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.of(author1));

        // Act
        Optional<Author> foundAuthor = authorService.findById(TEST_ID_1);

        // Assert
        assertThat(foundAuthor).isPresent().contains(author1);
        verify(cache).get(AUTHOR_CACHE_KEY_ID_1);
        verify(authorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("findById_whenNotCachedButExistsInRepo_shouldFetchAndCache")
    void findById_whenNotCachedButExistsInRepo_shouldFetchAndCache() {
        // Arrange
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));

        // Act
        Optional<Author> foundAuthor = authorService.findById(TEST_ID_1);

        // Assert
        assertThat(foundAuthor).isPresent().contains(author1);
        verify(cache).get(AUTHOR_CACHE_KEY_ID_1);
        verify(authorRepository).findById(TEST_ID_1);
        verify(cache).put(AUTHOR_CACHE_KEY_ID_1, author1);
    }

    @Test
    @DisplayName("findById_whenNotInCacheAndNotInRepo_shouldReturnEmpty")
    void findById_whenNotInCacheAndNotInRepo_shouldReturnEmpty() {
        // Arrange
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.empty());

        // Act
        Optional<Author> foundAuthor = authorService.findById(TEST_ID_1);

        // Assert
        assertThat(foundAuthor).isEmpty();
        verify(cache).get(AUTHOR_CACHE_KEY_ID_1);
        verify(authorRepository).findById(TEST_ID_1);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findByName_whenCachedAndMatches_shouldReturnCached")
    void findByName_whenCachedAndMatches_shouldReturnCached() {
        // Arrange
        when(cache.get(AUTHOR_CACHE_KEY_NAME_1)).thenReturn(Optional.of(author1));

        // Act
        Optional<Author> foundAuthor = authorService.findByName(AUTHOR_NAME_1);

        // Assert
        assertThat(foundAuthor).isPresent().contains(author1);
        verify(cache).get(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache, never()).evict(anyString()); // Ensure evict not called
        verify(authorRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo")
    void findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo() {
        // Arrange
        String requestedNameLower = AUTHOR_NAME_1.toLowerCase();
        String cacheKey = InMemoryCache.generateKey(StringConstants.AUTHOR, requestedNameLower);
        // Assume cache has the correct entity but under the lowercase key, but the entity name is proper case
        when(cache.get(cacheKey)).thenReturn(Optional.of(author1)); // author1.getName() is "AuthorOne"
        // Repo finds the correct entity for the lowercase name
        when(authorRepository.findByName(requestedNameLower)).thenReturn(Optional.of(author1));

        // Act
        Optional<Author> foundAuthor = authorService.findByName(requestedNameLower);

        // Assert
        assertThat(foundAuthor).isPresent().contains(author1);
        verify(cache).get(cacheKey);
        verify(cache).evict(cacheKey);
        verify(authorRepository).findByName(requestedNameLower);
        verify(cache).put(cacheKey, author1);
    }

    @Test
    @DisplayName("findByName_whenNotCachedButExistsInRepo_shouldFetchAndCache")
    void findByName_whenNotCachedButExistsInRepo_shouldFetchAndCache() {
        // Arrange
        when(cache.get(AUTHOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));

        // Act
        Optional<Author> foundAuthor = authorService.findByName(AUTHOR_NAME_1);

        // Assert
        assertThat(foundAuthor).isPresent().contains(author1);
        verify(cache).get(AUTHOR_CACHE_KEY_NAME_1);
        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(cache).put(AUTHOR_CACHE_KEY_NAME_1, author1);
    }

    @Test
    @DisplayName("findByName_whenNotInCacheAndNotInRepo_shouldReturnEmpty")
    void findByName_whenNotInCacheAndNotInRepo_shouldReturnEmpty() {
        // Arrange
        String nonExistentName = "NonExistentAuthor";
        String cacheKey = InMemoryCache.generateKey(StringConstants.AUTHOR, nonExistentName);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(authorRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

        // Act
        Optional<Author> foundAuthor = authorService.findByName(nonExistentName);

        // Assert
        assertThat(foundAuthor).isEmpty();
        verify(cache).get(cacheKey);
        verify(authorRepository).findByName(nonExistentName);
        verify(cache, never()).put(anyString(), any());
    }


    @Test
    @DisplayName("findDtoById_whenExists_shouldFetchAndConvertToDto")
    void findDtoById_whenExists_shouldFetchAndConvertToDto() {
        // Arrange
        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() { // Anonymous inner class for interface
                    public Long getId() {
                        return build1.getId();
                    }

                    public String getName() {
                        return build1.getName();
                    }
                }
        );
        // Mock findById behavior (which may involve cache or repo)
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached for this test

        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_1)).thenReturn(relatedBuilds);


        // Act
        Optional<AuthorDto> foundDto = authorService.findDtoById(TEST_ID_1);

        // Assert
        assertThat(foundDto).isPresent();
        assertThat(foundDto.get().id()).isEqualTo(TEST_ID_1);
        assertThat(foundDto.get().name()).isEqualTo(AUTHOR_NAME_1);
        assertThat(foundDto.get().relatedBuilds())
                .containsExactly(new RelatedBuildDto(build1.getId(), build1.getName()));

        // Verify underlying findById was called (and potentially cached)
        verify(authorRepository).findById(TEST_ID_1);
        verify(cache).put(AUTHOR_CACHE_KEY_ID_1, author1); // Verify caching from findById call

        // Verify build repo interaction for DTO conversion
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtoById_whenNotFound_shouldReturnEmpty")
    void findDtoById_whenNotFound_shouldReturnEmpty() {
        // Arrange
        when(authorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());

        // Act
        Optional<AuthorDto> foundDto = authorService.findDtoById(NON_EXISTENT_ID);

        // Assert
        assertThat(foundDto).isEmpty();
        verify(authorRepository).findById(NON_EXISTENT_ID);
        verify(buildRepository, never()).findBuildIdAndNameByAuthorId(anyLong());
    }


    @Test
    @DisplayName("findAllDtos_shouldFetchAllFromRepoAndConvert")
    void findAllDtos_shouldFetchAllFromRepoAndConvert() {
        // Arrange
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() {
                        return build1.getId();
                    }

                    public String getName() {
                        return build1.getName();
                    }
                }
        );
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = List.of(); // Author 2 has no builds in this setup


        when(authorRepository.findAll()).thenReturn(List.of(author1, author2));
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_2)).thenReturn(relatedBuilds2);


        // Act
        List<AuthorDto> allDtos = authorService.findAllDtos();

        // Assert
        assertThat(allDtos).hasSize(2);
        assertThat(allDtos.get(0).id()).isEqualTo(TEST_ID_1);
        assertThat(allDtos.get(0).name()).isEqualTo(AUTHOR_NAME_1);
        assertThat(allDtos.get(0).relatedBuilds()).hasSize(1);
        assertThat(allDtos.get(0).relatedBuilds().get(0).id()).isEqualTo(build1.getId());


        assertThat(allDtos.get(1).id()).isEqualTo(TEST_ID_2);
        assertThat(allDtos.get(1).name()).isEqualTo(AUTHOR_NAME_2);
        assertThat(allDtos.get(1).relatedBuilds()).isEmpty();

        verify(authorRepository).findAll();
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_2);
        // Verify getAll cache is NOT used (as per service implementation)
        verify(cache, never()).get(InMemoryCache.generateKey(StringConstants.AUTHOR, "ALL"));
        verify(cache, never()).put(InMemoryCache.generateKey(StringConstants.AUTHOR, "ALL"), any());
    }


    @Test
    @DisplayName("findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert")
    void findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert() {
        // Arrange
        String query = "Auth";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.AUTHOR, params);

        List<Author> repoResult = List.of(author1, author2);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of(); // Simplify for this test
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(authorRepository.fuzzyFindByName(query)).thenReturn(repoResult);
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_2)).thenReturn(relatedBuilds2);


        // Act
        List<AuthorDto> queryDtos = authorService.findDtosByNameQuery(query);

        // Assert
        assertThat(queryDtos).hasSize(2);
        assertThat(queryDtos.get(0).name()).isEqualTo(AUTHOR_NAME_1);
        assertThat(queryDtos.get(1).name()).isEqualTo(AUTHOR_NAME_2);

        verify(cache).get(queryKey);
        verify(authorRepository).fuzzyFindByName(query);
        verify(cache).put(queryKey, repoResult); // Verify caching of the repo result list
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_1); // Conversion check
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_2); // Conversion check
    }

    @Test
    @DisplayName("findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert")
    void findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert() {
        // Arrange
        String query = "Auth";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.AUTHOR, params);

        List<Author> cachedRepoResult = List.of(author1); // Cache has only author1
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.of(cachedRepoResult));
        // Mock DTO conversion dependency even when result is cached
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_1)).thenReturn(relatedBuilds1);


        // Act
        List<AuthorDto> queryDtos = authorService.findDtosByNameQuery(query);

        // Assert
        assertThat(queryDtos).hasSize(1);
        assertThat(queryDtos.get(0).name()).isEqualTo(AUTHOR_NAME_1);


        verify(cache).get(queryKey);
        verify(authorRepository, never()).fuzzyFindByName(any()); // Repo not called
        verify(cache, never()).put(anyString(), any()); // Cache not updated
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_1); // Conversion still happens
    }

    @Test
    @DisplayName("findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery")
    void findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery() {
        // Arrange
        // FIX 2: Add comment explaining why 'query = null' is intentional for this test
        // Variable 'query' is intentionally null to test handling of null search parameters.
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, "__NULL__"); // Expect null placeholder
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.AUTHOR, params);

        List<Author> repoResult = List.of(author1, author2); // Assume null query returns all
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(authorRepository.fuzzyFindByName(null)).thenReturn(repoResult); // Pass null to repo method
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByAuthorId(TEST_ID_2)).thenReturn(relatedBuilds2);


        // Act
        List<AuthorDto> queryDtos = authorService.findDtosByNameQuery(null);

        // Assert
        assertThat(queryDtos).hasSize(2);

        verify(cache).get(queryKey);
        verify(authorRepository).fuzzyFindByName(null); // Verify null passed to repo
        verify(cache).put(queryKey, repoResult);
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByAuthorId(TEST_ID_2);
    }


    @Test
    @DisplayName("update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries")
    void update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries() {
        // Arrange
        Author updatedAuthor = createTestAuthor(TEST_ID_1, TEST_NAME_NEW);
        // Cache key for the original name
        String newNameCacheKey = InMemoryCache.generateKey(StringConstants.AUTHOR, TEST_NAME_NEW);
        String idCacheKey = AUTHOR_CACHE_KEY_ID_1;

        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1)); // findById finds the original
        when(authorRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty()); // New name is unique
        when(authorRepository.save(any(Author.class))).thenReturn(updatedAuthor); // save returns the updated entity
        when(cache.get(idCacheKey)).thenReturn(Optional.empty()); // Assume findById doesn't hit cache initially

        // Act
        Author result = authorService.update(TEST_ID_1, TEST_NAME_NEW);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);
        verify(authorRepository).findById(TEST_ID_1);
        verify(authorRepository).findByName(TEST_NAME_NEW);
        verify(authorRepository).save(authorCaptor.capture());
        assertThat(authorCaptor.getValue().getId()).isEqualTo(TEST_ID_1);
        assertThat(authorCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW);

        // --- Cache Verification (Refined) ---
        verify(cache).evict(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache).put(newNameCacheKey, updatedAuthor);
        ArgumentCaptor<Author> idPutCaptor = ArgumentCaptor.forClass(Author.class);
        verify(cache, atLeastOnce()).put(eq(idCacheKey), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
    }

    @Test
    @DisplayName("update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName") // New Test for Branch
    void update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName() {
        // Simulate some other field changed if Author had more fields, or just save triggers update
        Author savedAuthor = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1); // Represents state after save

        String nameCacheKey = AUTHOR_CACHE_KEY_NAME_1;
        String idCacheKey = AUTHOR_CACHE_KEY_ID_1;

        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        // findByName will find the *same* entity, so the uniqueness check passes
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);
        when(cache.get(idCacheKey)).thenReturn(Optional.empty()); // Assume findById cache miss

        // Act
        Author result = authorService.update(TEST_ID_1, AUTHOR_NAME_1); // Update with same name

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(AUTHOR_NAME_1);

        verify(authorRepository).findById(TEST_ID_1);
        verify(authorRepository).findByName(AUTHOR_NAME_1); // Uniqueness check still happens
        verify(authorRepository).save(any(Author.class));

        // Cache Verification
        // *** Verify OLD name cache key was NOT evicted ***
        verify(cache, never()).evict(nameCacheKey);
        // Verify name cache key WAS updated (put) with potentially new object state
        verify(cache).put(nameCacheKey, savedAuthor);
        // Verify ID cache was updated (at least once)
        ArgumentCaptor<Author> idPutCaptor = ArgumentCaptor.forClass(Author.class);
        verify(cache, atLeastOnce()).put(eq(idCacheKey), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getName()).isEqualTo(AUTHOR_NAME_1); // Ensure correct entity state
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
    }

    @Test
    @DisplayName("update_whenNameExistsForSameId_shouldSucceed") // New Test for Branch
    void update_whenNameExistsForSameId_shouldSucceed() {
        // Arrange: Trying to update author1 with a name that already belongs to author1
        Author savedAuthor = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);

        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        // findByName finds the *same* entity being updated
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());

        // Act
        Author result = authorService.update(TEST_ID_1, AUTHOR_NAME_1);

        // Assert: Should succeed without exception
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(AUTHOR_NAME_1);

        // Verify flow - findById, findByName, save all called
        verify(authorRepository).findById(TEST_ID_1);
        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(authorRepository).save(any(Author.class));
        // Verify cache operations (similar to name not changing)
        verify(cache, never()).evict(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache).put(AUTHOR_CACHE_KEY_NAME_1, savedAuthor);
        verify(cache, atLeastOnce()).put(AUTHOR_CACHE_KEY_ID_1, savedAuthor);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
    }


    @Test
    @DisplayName("update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException")
    void update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException() {
        // Arrange
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        when(authorRepository.findByName(AUTHOR_NAME_2)).thenReturn(Optional.of(author2));
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume findById not cached

        // Act & Assert
        assertThatThrownBy(() -> authorService.update(TEST_ID_1, AUTHOR_NAME_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(AUTHOR_NAME_2
                        + "' " + StringConstants.ALREADY_EXISTS_MESSAGE);

        verify(authorRepository).findById(TEST_ID_1);
        verify(cache).put(AUTHOR_CACHE_KEY_ID_1, author1); // findById still caches

        verify(authorRepository).findByName(AUTHOR_NAME_2);
        verify(authorRepository, never()).save(any());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("update_whenNotFound_shouldThrowNoSuchElementException")
    void update_whenNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        when(authorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authorService.update(NON_EXISTENT_ID, TEST_NAME_NEW))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.AUTHOR + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(authorRepository).findById(NON_EXISTENT_ID);
        verify(authorRepository, never()).findByName(anyString());
        verify(authorRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById_whenAuthorExistsAndIsLastAuthorOfBuild_shouldDeleteAuthorAndBuildAndEvictCaches")
    void deleteById_whenAuthorExistsAndIsLastAuthorOfBuild_shouldDeleteAuthorAndBuildAndEvictCaches() {
        // Arrange
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        when(buildRepository.findBuildsByAuthorId(TEST_ID_1)).thenReturn(List.of(build2OnlyAuthor1));
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());

        // Act
        authorService.deleteById(TEST_ID_1);

        // Assert
        verify(authorRepository).delete(author1);
        verify(buildRepository).delete(build2OnlyAuthor1);
        verify(buildRepository, never()).save(any(Build.class));
        verify(cache).evict(AUTHOR_CACHE_KEY_ID_1);
        verify(cache).evict(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
        verify(cache).evict(InMemoryCache.generateKey(StringConstants.BUILD, build2OnlyAuthor1.getId()));
        verify(cache).evict(InMemoryCache.generateKey(StringConstants.BUILD, build2OnlyAuthor1.getName()));
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }


    @Test
    @DisplayName("deleteById_whenAuthorExistsAndIsNotLastAuthorOfBuild_shouldDeleteAuthorAndUpdateBuildAndEvictCaches")
    void deleteById_whenAuthorExistsAndIsNotLastAuthorOfBuild_shouldDeleteAuthorAndUpdateBuildAndEvictCaches() {
        // Arrange
        when(authorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(author1));
        when(buildRepository.findBuildsByAuthorId(TEST_ID_1)).thenReturn(List.of(build1));
        when(cache.get(AUTHOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authorService.deleteById(TEST_ID_1);

        // Assert
        verify(authorRepository).delete(author1);
        verify(buildRepository).save(buildCaptor.capture());
        Build savedBuild = buildCaptor.getValue();
        assertThat(savedBuild.getAuthors()).doesNotContain(author1);
        assertThat(savedBuild.getAuthors()).contains(author2);
        verify(buildRepository, never()).delete(any(Build.class));
        verify(cache).evict(AUTHOR_CACHE_KEY_ID_1);
        verify(cache).evict(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.BUILD, savedBuild.getId()), savedBuild);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.BUILD, savedBuild.getName()), savedBuild);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteById_whenAuthorExistsAndHasNoBuilds_shouldDeleteAuthorAndEvictCaches") // New Test for Branch
    void deleteById_whenAuthorExistsAndHasNoBuilds_shouldDeleteAuthorAndEvictCaches() {
        // Arrange: author2 is not associated with any builds in this setup initially
        author2 = createTestAuthor(TEST_ID_2, AUTHOR_NAME_2);
        when(authorRepository.findById(TEST_ID_2)).thenReturn(Optional.of(author2));
        // *** Crucial: Mock build repo to return empty list for this author ***
        when(buildRepository.findBuildsByAuthorId(TEST_ID_2)).thenReturn(Collections.emptyList());
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, TEST_ID_2))).thenReturn(Optional.empty()); // Assume findById cache miss

        // Act
        authorService.deleteById(TEST_ID_2);

        // Assert
        // Verify author deletion
        verify(authorRepository).delete(author2);
        // Verify build repo interactions stopped early
        verify(buildRepository).findBuildsByAuthorId(TEST_ID_2); // Check was performed
        verify(buildRepository, never()).delete(any(Build.class)); // No builds deleted
        verify(buildRepository, never()).save(any(Build.class)); // No builds saved

        // Verify cache evictions
        verify(cache).evict(InMemoryCache.generateKey(StringConstants.AUTHOR, TEST_ID_2));
        verify(cache).evict(InMemoryCache.generateKey(StringConstants.AUTHOR, AUTHOR_NAME_2));
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
        // *** Verify Build Query Cache NOT evicted because buildCacheInvalidated is false ***
        verify(cache, never()).evictQueryCacheByType(StringConstants.BUILD);
    }


    @Test
    @DisplayName("deleteById_whenNotFound_shouldThrowNoSuchElementException")
    void deleteById_whenNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        when(authorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authorService.deleteById(NON_EXISTENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.AUTHOR + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(authorRepository).findById(NON_EXISTENT_ID);
        verify(authorRepository, never()).delete(any());
        verify(buildRepository, never()).findBuildsByAuthorId(anyLong());
        verify(cache, never()).evict(anyString());
    }

    @Test
    @DisplayName("deleteByName_whenExists_shouldDeleteAndEvict")
    void deleteByName_whenExists_shouldDeleteAndEvict() {
        // Arrange
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));
        when(buildRepository.findBuildsByAuthorId(author1.getId())).thenReturn(List.of(build2OnlyAuthor1)); // Associated build
        when(cache.get(AUTHOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());

        // Act
        authorService.deleteByName(AUTHOR_NAME_1);

        // Assert
        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(buildRepository).findBuildsByAuthorId(author1.getId());
        verify(buildRepository).delete(build2OnlyAuthor1);
        verify(authorRepository).delete(author1);
        verify(cache).evict(AUTHOR_CACHE_KEY_ID_1);
        verify(cache).evict(AUTHOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteByName_whenNotFound_shouldThrowNoSuchElementException")
    void deleteByName_whenNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        when(authorRepository.findByName(TEST_NAME_NON_EXISTENT)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, TEST_NAME_NON_EXISTENT))).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authorService.deleteByName(TEST_NAME_NON_EXISTENT))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.AUTHOR + " " + StringConstants.WITH_NAME + " '" + TEST_NAME_NON_EXISTENT + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(authorRepository).findByName(TEST_NAME_NON_EXISTENT);
        verify(authorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("findOrCreate_whenExists_shouldReturnExisting")
    void findOrCreate_whenExists_shouldReturnExisting() {
        // Arrange
        when(authorRepository.findByName(AUTHOR_NAME_1)).thenReturn(Optional.of(author1));
        when(cache.get(AUTHOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());

        // Act
        Author result = authorService.findOrCreate(AUTHOR_NAME_1);

        // Assert
        assertThat(result).isEqualTo(author1);
        verify(authorRepository).findByName(AUTHOR_NAME_1);
        verify(authorRepository, never()).save(any());
        verify(cache).put(AUTHOR_CACHE_KEY_NAME_1, author1);
    }

    @Test
    @DisplayName("findOrCreate_whenNotExists_shouldCreateAndReturnNew")
    void findOrCreate_whenNotExists_shouldCreateAndReturnNew() {
        // Arrange
        String newName = "NewAuthorToCreate";
        Author newlyCreatedAuthor = createTestAuthor(TEST_ID_3, newName);

        when(authorRepository.findByName(newName)).thenReturn(Optional.empty());
        when(authorRepository.save(any(Author.class))).thenReturn(newlyCreatedAuthor);
        when(cache.get(InMemoryCache.generateKey(StringConstants.AUTHOR, newName))).thenReturn(Optional.empty());

        // Act
        Author result = authorService.findOrCreate(newName);

        // Assert
        assertThat(result).isEqualTo(newlyCreatedAuthor);
        verify(authorRepository, times(2)).findByName(newName);
        verify(authorRepository).save(authorCaptor.capture());
        assertThat(authorCaptor.getValue().getName()).isEqualTo(newName);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, newlyCreatedAuthor.getId()), newlyCreatedAuthor);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, newName), newlyCreatedAuthor);
        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
    }

    @Test
    @DisplayName("createBulk_whenSomeExist_shouldCreateNewAndSkipExisting")
    void createBulk_whenSomeExist_shouldCreateNewAndSkipExisting() {
        // Arrange
        List<String> namesToCreate = List.of(AUTHOR_NAME_1, AUTHOR_NAME_2, AUTHOR_NAME_3, "  " + AUTHOR_NAME_1 + "  ");
        Set<String> uniqueLowerNames = Set.of(AUTHOR_NAME_1.toLowerCase(), AUTHOR_NAME_2.toLowerCase(), AUTHOR_NAME_3.toLowerCase());
        Set<Author> existingAuthors = Set.of(author1);

        Author author3 = createTestAuthor(TEST_ID_3, AUTHOR_NAME_3);
        List<Author> savedEntities = List.of(author2, author3);


        when(authorRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingAuthors);
        when(authorRepository.saveAll(anyList())).thenReturn(savedEntities);


        // Act
        BulkCreationResult<String> result = authorService.createBulk(namesToCreate);

        // Assert
        assertThat(result.createdItems()).containsExactlyInAnyOrder(AUTHOR_NAME_2, AUTHOR_NAME_3);
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(AUTHOR_NAME_1);

        verify(authorRepository).findByNamesIgnoreCase(uniqueLowerNames);

        verify(authorRepository).saveAll(authorListCaptor.capture());
        List<Author> authorsPassedToSaveAll = authorListCaptor.getValue();
        assertThat(authorsPassedToSaveAll).hasSize(2);
        assertThat(authorsPassedToSaveAll.stream().map(Author::getName)).containsExactlyInAnyOrder(AUTHOR_NAME_2, AUTHOR_NAME_3);

        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, author2.getId()), author2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, author2.getName()), author2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, author3.getId()), author3);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.AUTHOR, author3.getName()), author3);

        verify(cache).evictQueryCacheByType(StringConstants.AUTHOR);
    }

    @Test
    @DisplayName("createBulk_whenAllExist_shouldSkipAll")
    void createBulk_whenAllExist_shouldSkipAll() {
        // Arrange
        List<String> namesToCreate = List.of(AUTHOR_NAME_1, AUTHOR_NAME_2);
        Set<String> uniqueLowerNames = Set.of(AUTHOR_NAME_1.toLowerCase(), AUTHOR_NAME_2.toLowerCase());
        Set<Author> existingAuthors = Set.of(author1, author2);

        when(authorRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingAuthors);

        // Act
        BulkCreationResult<String> result = authorService.createBulk(namesToCreate);

        // Assert
        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(AUTHOR_NAME_1, AUTHOR_NAME_2);

        verify(authorRepository).findByNamesIgnoreCase(uniqueLowerNames);
        verify(authorRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn")
    void createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn() {
        // Arrange
        // Use Arrays.asList() which allows nulls for input list
        List<String> namesToCreate = Arrays.asList(null, "   ", "", null);

        // Act
        BulkCreationResult<String> result = authorService.createBulk(namesToCreate);
        // Getting skippedItems here is fine, or you could assert on result.skippedItems() directly
        List<String> skippedItems = result.skippedItems();

        // Assert
        assertThat(result.createdItems())
                .as("Created items list should be empty")
                .isEmpty(); // Assertion on createdItems (separate subject)

        // Chain all assertions related to skippedItems
        assertThat(skippedItems)
                .as("Skipped items list check") // Use one description for the whole chain
                .isNotNull() // Check it's not null
                .hasSize(4) // Check the size
                // Check the exact content, allowing for any order and duplicates (like null)
                .containsExactlyInAnyOrder(null, "   ", "", null);

        // Verify repository and cache were not interacted with
        verify(authorRepository, never()).findByNamesIgnoreCase(any());
        verify(authorRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withNullInputList_shouldReturnEmptyResults")
    void createBulk_withNullInputList_shouldReturnEmptyResults() {
        // Act
        BulkCreationResult<String> result = authorService.createBulk(null);

        // Assert
        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).isEmpty();
        verifyNoInteractions(authorRepository);
        verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("cacheEntity_withNullEntity_shouldLogWarningAndReturn")
    void cacheEntity_withNullEntity_shouldLogWarningAndReturn() {
        // Act
        authorService.cacheEntity(null);
        // Assert
        verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("cacheEntity_withNullId_shouldLogWarningAndReturn")
    void cacheEntity_withNullId_shouldLogWarningAndReturn() {
        // Arrange
        Author authorWithNullId = Author.builder().name(AUTHOR_NAME_1).id(null).build();
        // Act
        authorService.cacheEntity(authorWithNullId);
        // Assert
        verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("cacheEntity_withNullName_shouldLogWarningAndReturn")
    void cacheEntity_withNullName_shouldLogWarningAndReturn() {
        // Arrange
        Author authorWithNullName = Author.builder().name(null).id(TEST_ID_1).build();
        // Act
        authorService.cacheEntity(authorWithNullName);
        // Assert
        verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("evictEntityById_withNullId_shouldReturn")
    void evictEntityById_withNullId_shouldReturn() {
        // Act
        authorService.evictEntityById(null);
        // Assert
        verifyNoInteractions(cache);
    }

    @Test
    @DisplayName("evictEntityByName_withNullName_shouldReturn")
    void evictEntityByName_withNullName_shouldReturn() {
        // Act
        authorService.evictEntityByName(null);
        // Assert
        verifyNoInteractions(cache);
    }

    // --- Test Cases Specific to AuthorService ---

    @Test
    @DisplayName("convertToDto_shouldFetchRelatedBuildsAndConvert")
    void convertToDto_shouldFetchRelatedBuildsAndConvert() {
        // Arrange
        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() {
                        return build1.getId();
                    }

                    public String getName() {
                        return build1.getName();
                    }
                },
                new BuildRepository.BuildIdAndName() {
                    public Long getId() {
                        return build2OnlyAuthor1.getId();
                    }

                    public String getName() {
                        return build2OnlyAuthor1.getName();
                    }
                }
        );
        when(buildRepository.findBuildIdAndNameByAuthorId(author1.getId())).thenReturn(relatedBuilds);

        // Act
        AuthorDto dto = authorService.convertToDto(author1);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(author1.getId());
        assertThat(dto.name()).isEqualTo(author1.getName());
        assertThat(dto.relatedBuilds()).hasSize(2);
        assertThat(dto.relatedBuilds().get(0)).isEqualTo(new RelatedBuildDto(build1.getId(), build1.getName()));
        assertThat(dto.relatedBuilds().get(1)).isEqualTo(new RelatedBuildDto(build2OnlyAuthor1.getId(), build2OnlyAuthor1.getName()));


        verify(buildRepository).findBuildIdAndNameByAuthorId(author1.getId());
    }

    @Test
    @DisplayName("convertToDto_withNullAuthor_shouldReturnNull")
    void convertToDto_withNullAuthor_shouldReturnNull() {
        // Act
        AuthorDto dto = authorService.convertToDto(null);

        // Assert
        assertThat(dto).isNull();
        verifyNoInteractions(buildRepository);
    }

    @Test
    @DisplayName("checkDeletionConstraints_forAuthor_shouldDoNothing")
    void checkDeletionConstraints_forAuthor_shouldDoNothing() {
        // Arrange
        // Act
        authorService.checkDeletionConstraints(author1);
        // Assert
        verify(authorRepository, never()).delete(any());
        verify(cache, never()).evict(anyString());
    }


    @Test
    @DisplayName("instantiateEntity_shouldReturnNewAuthorWithName")
    void instantiateEntity_shouldReturnNewAuthorWithName() {
        // Arrange
        String name = "Newbie";

        // Act
        Author newAuthor = authorService.instantiateEntity(name);

        // Assert
        assertThat(newAuthor).isNotNull();
        assertThat(newAuthor.getId()).isNull();
        assertThat(newAuthor.getName()).isEqualTo(name);
    }
}