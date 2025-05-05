// file: src/test/java/sovok/mcbuildlibrary/service/BuildServiceTest.java
package sovok.mcbuildlibrary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static sovok.mcbuildlibrary.TestConstants.AUTHOR_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.AUTHOR_NAME_2;
import static sovok.mcbuildlibrary.TestConstants.BUILD_CACHE_KEY_ID_1;
import static sovok.mcbuildlibrary.TestConstants.BUILD_CACHE_KEY_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.BUILD_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.BUILD_NAME_2;
import static sovok.mcbuildlibrary.TestConstants.COLOR_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.NON_EXISTENT_ID;
import static sovok.mcbuildlibrary.TestConstants.TEST_ID_1;
import static sovok.mcbuildlibrary.TestConstants.TEST_ID_2;
import static sovok.mcbuildlibrary.TestConstants.TEST_NAME_NEW;
import static sovok.mcbuildlibrary.TestConstants.TEST_NAME_NON_EXISTENT;
import static sovok.mcbuildlibrary.TestConstants.TEST_SCHEM_BYTES;
import static sovok.mcbuildlibrary.TestConstants.THEME_NAME_1;
import static sovok.mcbuildlibrary.TestConstants.createTestAuthor;
import static sovok.mcbuildlibrary.TestConstants.createTestBuild;
import static sovok.mcbuildlibrary.TestConstants.createTestColor;
import static sovok.mcbuildlibrary.TestConstants.createTestTheme;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
// import org.mockito.Spy; // Removed @Spy annotation
import org.mockito.junit.jupiter.MockitoExtension;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;

/**
 * Unit tests for the {@link BuildService}. Covers creation, finding, filtering, updating,
 * deleting, schematic retrieval, and cache interactions for Builds.
 */
@ExtendWith(MockitoExtension.class)
class BuildServiceTest {

    @Mock
    private BuildRepository buildRepository;
    @Mock
    private InMemoryCache cache;

    @InjectMocks
    private BuildService buildService; // Instance created by Mockito via @InjectMocks

    // Spy will be created manually in setUp
    private BuildService buildServiceSpy;

    @Captor
    private ArgumentCaptor<Build> buildCaptor;

    private Build build1;
    private Build build2;

    @BeforeEach
    void setUp() {
        Author author1 = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);
        Author author2 = createTestAuthor(TEST_ID_2, AUTHOR_NAME_2);
        Theme theme1 = createTestTheme(TEST_ID_1, THEME_NAME_1);
        Color color1 = createTestColor(TEST_ID_1, COLOR_NAME_1);

        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(author1), Set.of(theme1),
                Set.of(color1));
        build2 = createTestBuild(TEST_ID_2, BUILD_NAME_2, Set.of(author2), Set.of(theme1),
                Set.of(color1));

        // --- FIX: Manually create the spy ---
        buildServiceSpy = spy(buildService);
        // Inject the spy for self-invocation testing
        buildService.setSelf(buildServiceSpy);
    }

    // --- createBuild Tests ---

    @Test
    @DisplayName("createBuild: Success - Should save and cache when name is unique")
    void createBuild_whenNameDoesNotExist_shouldSaveAndCache() {
        // Arrange
        Build buildToCreate = createTestBuild(null, BUILD_NAME_1, build1.getAuthors(),
                build1.getThemes(), build1.getColors()); // No ID before creation
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.empty());
        when(buildRepository.save(any(Build.class))).thenReturn(build1); // Return build with ID

        // Act
        Build createdBuild = buildService.createBuild(buildToCreate);

        // Assert
        assertThat(createdBuild).isNotNull();
        assertThat(createdBuild.getId()).isEqualTo(TEST_ID_1);
        assertThat(createdBuild.getName()).isEqualTo(BUILD_NAME_1);

        verify(buildRepository).findByName(BUILD_NAME_1);
        verify(buildRepository).save(buildCaptor.capture());
        assertThat(buildCaptor.getValue().getName()).isEqualTo(BUILD_NAME_1);
        assertThat(buildCaptor.getValue().getId()).isNull(); // ID should be null before save

        verify(cache).put(BUILD_CACHE_KEY_ID_1, createdBuild);
        verify(cache).put(BUILD_CACHE_KEY_NAME_1, createdBuild);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("createBuild: Failure - Should throw exception when name already exists")
    void createBuild_whenNameExists_shouldThrowIllegalArgumentException() {
        // Arrange
        Build buildWithNoId = createTestBuild(null, BUILD_NAME_1, Set.of(), Set.of(), Set.of());
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.of(build1));

        // Act & Assert
        assertThatThrownBy(() -> buildService.createBuild(buildWithNoId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format(StringConstants
                                .RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        StringConstants.BUILD, StringConstants.WITH_NAME, BUILD_NAME_1,
                        StringConstants.ALREADY_EXISTS_MESSAGE));

        verify(buildRepository).findByName(BUILD_NAME_1);
        verify(buildRepository, never()).save(any());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    // --- findBuildById Tests ---

    @Test
    @DisplayName("findBuildById: Cache Hit - Should return cached build")
    void findBuildById_whenCached_shouldReturnCached() {
        // Arrange
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findBuildById(TEST_ID_1);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(BUILD_CACHE_KEY_ID_1);
        verify(buildRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("findBuildById: Cache Miss, DB Hit - Should fetch from repo and cache")
    void findBuildById_whenNotCachedAndExists_shouldFetchAndCache() {
        // Arrange
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findBuildById(TEST_ID_1);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(BUILD_CACHE_KEY_ID_1);
        verify(buildRepository).findById(TEST_ID_1);
        verify(cache).put(BUILD_CACHE_KEY_ID_1, build1);
    }

    @Test
    @DisplayName("findBuildById: Not Found - Should return empty optional")
    void findBuildById_whenNotFound_shouldReturnEmpty() {
        // Arrange
        String cacheKey = InMemoryCache.generateKey(StringConstants.BUILD, NON_EXISTENT_ID);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<Build> foundBuild = buildService.findBuildById(NON_EXISTENT_ID);

        // Assert
        assertThat(foundBuild).isEmpty();
        verify(cache).get(cacheKey);
        verify(buildRepository).findById(NON_EXISTENT_ID);
        verify(cache, never()).put(anyString(), any());
    }

    // --- findByName Tests ---

    @Test
    @DisplayName("findByName: Cache Hit - Should return cached build when name matches")
    void findByName_whenCachedAndMatches_shouldReturnCached() {
        // Arrange
        when(cache.get(BUILD_CACHE_KEY_NAME_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findByName(BUILD_NAME_1);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(BUILD_CACHE_KEY_NAME_1);
        verify(cache, never()).evict(anyString());
        verify(buildRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findByName: Cache Hit, Mismatch - Should evict cache and fetch from repo")
    void findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo() {
        // Arrange
        String requestedNameLower = BUILD_NAME_1.toLowerCase(); // e.g., "buildone"
        String cacheKey = InMemoryCache.generateKey(StringConstants.BUILD, requestedNameLower);
        // Simulate cache has the correct build object but under the lowercase key
        when(cache.get(cacheKey)).thenReturn(Optional.of(build1)); // build1.getName() is "BuildOne"
        // Repo returns the correct build for the requested name
        when(buildRepository.findByName(requestedNameLower)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findByName(requestedNameLower);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(cacheKey);
        // Verify cache is evicted because cached name "BuildOne" != requested name "buildone"
        verify(cache).evict(cacheKey);
        verify(buildRepository).findByName(requestedNameLower);
        // Verify cache is updated with the correct key-value pair
        verify(cache).put(cacheKey, build1);
    }

    @Test
    @DisplayName("findByName: Cache Miss, DB Hit - Should fetch from repo and cache")
    void findByName_whenNotCachedAndExists_shouldFetchAndCache() {
        // Arrange
        when(cache.get(BUILD_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findByName(BUILD_NAME_1);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(BUILD_CACHE_KEY_NAME_1);
        verify(buildRepository).findByName(BUILD_NAME_1);
        verify(cache).put(BUILD_CACHE_KEY_NAME_1, build1);
    }

    @Test
    @DisplayName("findByName: Not Found - Should return empty optional")
    void findByName_whenNotFound_shouldReturnEmpty() {
        // Arrange
        String cacheKey = InMemoryCache.generateKey(StringConstants.BUILD, TEST_NAME_NON_EXISTENT);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findByName(TEST_NAME_NON_EXISTENT)).thenReturn(Optional.empty());

        // Act
        Optional<Build> foundBuild = buildService.findByName(TEST_NAME_NON_EXISTENT);

        // Assert
        assertThat(foundBuild).isEmpty();
        verify(cache).get(cacheKey);
        verify(buildRepository).findByName(TEST_NAME_NON_EXISTENT);
        verify(cache, never()).put(anyString(), any());
    }

    // --- findAll Tests ---

    @Test
    @DisplayName("findAll: Should return all builds directly from repository")
    void findAll_shouldReturnFromRepositoryDirectly() {
        // Arrange
        List<Build> expectedBuilds = List.of(build1, build2);
        when(buildRepository.findAll()).thenReturn(expectedBuilds);

        // Act
        List<Build> actualBuilds = buildService.findAll();

        // Assert
        assertThat(actualBuilds).isEqualTo(expectedBuilds);
        verify(buildRepository).findAll();
        verify(cache, never()).get(anyString()); // findAll bypasses cache
        verify(cache, never()).put(anyString(), any());
    }

    // --- filterBuilds Tests ---

    @Test
    @DisplayName("filterBuilds: Cache Miss - Should query repo and cache result")
    void filterBuilds_whenNotCached_shouldQueryRepoAndCache() {
        // Arrange
        String authorQuery = "Auth";
        String themeQuery = "Theme";
        List<Build> repoResult = List.of(build1);

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", null);
        params.put("theme", themeQuery);
        params.put("color", null);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(buildRepository.fuzzyFilterBuilds(authorQuery, null, themeQuery, null))
                .thenReturn(repoResult);

        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, null,
                themeQuery, null);

        // Assert
        assertThat(filteredBuilds).isEqualTo(repoResult);
        verify(cache).get(queryKey);
        verify(buildRepository).fuzzyFilterBuilds(authorQuery, null, themeQuery, null);
        verify(cache).put(queryKey, repoResult);
    }

    @Test
    @DisplayName("filterBuilds: Cache Hit - Should return cached result")
    void filterBuilds_whenCached_shouldReturnCachedResult() {
        // Arrange
        String authorQuery = "Auth";
        String themeQuery = "Theme";
        List<Build> cachedResult = List.of(build1, build2);

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", null);
        params.put("theme", themeQuery);
        params.put("color", null);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);

        when(cache.get(queryKey)).thenReturn(Optional.of(cachedResult));

        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, null,
                themeQuery, null);

        // Assert
        assertThat(filteredBuilds).isEqualTo(cachedResult);
        verify(cache).get(queryKey);
        verify(buildRepository, never()).fuzzyFilterBuilds(any(), any(), any(), any());
        verify(cache, never()).put(anyString(), any()); // Don't put again if cache hit
    }

    @Test
    @DisplayName("filterBuilds: Empty Result - Should query repo and cache empty list")
    void filterBuilds_whenReturnsEmptyList_shouldCacheEmptyList() {
        // Arrange
        String authorQuery = "NonExistentAuthor";
        List<Build> repoResult = Collections.emptyList();

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", null);
        params.put("theme", null);
        params.put("color", null);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(buildRepository.fuzzyFilterBuilds(authorQuery, null, null, null))
                .thenReturn(repoResult);

        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, null, null, null);

        // Assert
        assertThat(filteredBuilds).isEmpty();
        verify(cache).get(queryKey);
        verify(buildRepository).fuzzyFilterBuilds(authorQuery, null, null, null);
        // IMPORTANT: Verify that even an empty list gets cached
        verify(cache).put(queryKey, repoResult);
    }

    // --- getSchemFile Tests ---

    @Test
    @DisplayName("getSchemFile: Success - Should return bytes when build and file exist")
    void getSchemFile_whenBuildExistsAndHasFile_shouldReturnBytes() {
        // Arrange
        // Use the spy to mock the internal self-call to findBuildById
        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isPresent().contains(TEST_SCHEM_BYTES);
        verify(buildServiceSpy).findBuildById(TEST_ID_1); // Verify the internal call on the spy
    }

    @Test
    @DisplayName("getSchemFile: No File - Should return empty when build exists but schem is null")
    void getSchemFile_whenBuildExistsButNoFile_shouldReturnEmpty() {
        // Arrange
        Build buildWithoutSchem = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(), Set.of(),
                Set.of());
        buildWithoutSchem.setSchemFile(null);
        doReturn(Optional.of(buildWithoutSchem)).when(buildServiceSpy).findBuildById(TEST_ID_1);

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isEmpty();
        verify(buildServiceSpy).findBuildById(TEST_ID_1);
    }

    @Test
    @DisplayName("getSchemFile: Empty File - Should return empty when build exists but schem is empty array")
    void getSchemFile_whenBuildExistsButEmptyFile_shouldReturnEmpty() {
        // Arrange
        Build buildWithEmptySchem = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(), Set.of(),
                Set.of());
        buildWithEmptySchem.setSchemFile(new byte[0]); // Empty byte array
        doReturn(Optional.of(buildWithEmptySchem)).when(buildServiceSpy).findBuildById(TEST_ID_1);

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isEmpty(); // Should be empty due to the filter
        verify(buildServiceSpy).findBuildById(TEST_ID_1);
    }

    @Test
    @DisplayName("getSchemFile: Build Not Found - Should return empty")
    void getSchemFile_whenBuildNotFound_shouldReturnEmpty() {
        // Arrange
        doReturn(Optional.empty()).when(buildServiceSpy).findBuildById(NON_EXISTENT_ID);

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(NON_EXISTENT_ID);

        // Assert
        assertThat(schemBytes).isEmpty();
        verify(buildServiceSpy).findBuildById(NON_EXISTENT_ID);
    }

    // --- updateBuild Tests ---

    @Test
    @DisplayName("updateBuild: Success, Name Changed - Should update, evict old name cache, put new caches")
    void updateBuild_whenExistsAndNameUnique_shouldUpdateAndManageCache() {
        // Arrange
        Build updatedBuildData = Build.builder()
                .name(TEST_NAME_NEW) // New name
                .authors(build1.getAuthors())
                .themes(build1.getThemes())
                .colors(build1.getColors())
                .description("Updated Desc")
                .screenshots(List.of("new.png"))
                .schemFile("new schem".getBytes())
                .build();
        // This represents the state *after* save
        Build savedBuild = createTestBuild(TEST_ID_1, TEST_NAME_NEW, build1.getAuthors(),
                build1.getThemes(), build1.getColors());
        savedBuild.setDescription("Updated Desc");
        savedBuild.setScreenshots(List.of("new.png"));
        savedBuild.setSchemFile("new schem".getBytes());
        String newNameCacheKey = InMemoryCache.generateKey(StringConstants.BUILD, TEST_NAME_NEW);

        // Mock the internal findBuildById call via the spy
        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);
        when(buildRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty()); // New name is unique
        when(buildRepository.save(any(Build.class))).thenReturn(savedBuild);

        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);
        assertThat(result.getDescription()).isEqualTo("Updated Desc");
        assertThat(result.getSchemFile()).isEqualTo("new schem".getBytes());

        verify(buildServiceSpy).findBuildById(TEST_ID_1); // Verify internal call
        verify(buildRepository).findByName(TEST_NAME_NEW);
        verify(buildRepository).save(buildCaptor.capture());
        Build buildPassedToSave = buildCaptor.getValue();
        assertThat(buildPassedToSave.getId()).isEqualTo(TEST_ID_1); // Ensure ID was preserved
        assertThat(buildPassedToSave.getName()).isEqualTo(TEST_NAME_NEW);
        assertThat(buildPassedToSave.getDescription()).isEqualTo("Updated Desc");
        assertThat(buildPassedToSave.getSchemFile()).isEqualTo("new schem".getBytes());

        // Cache Verifications
        verify(cache).evict(BUILD_CACHE_KEY_NAME_1); // Evict old name cache
        verify(cache).put(newNameCacheKey, savedBuild); // Put new name cache
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(BUILD_CACHE_KEY_ID_1), idPutCaptor.capture()); // Update ID cache
        assertThat(idPutCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW); // Check updated data in ID cache
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("updateBuild: Success, Name Unchanged - Should update, NOT evict name cache, update caches")
    void updateBuild_whenNameDoesNotChange_shouldUpdateAndManageCache() {
        // Arrange
        Build updatedBuildData = Build.builder()
                .name(BUILD_NAME_1) // SAME name
                .authors(build1.getAuthors())
                .themes(build1.getThemes())
                .colors(build1.getColors())
                .description("New Description Same Name") // Different description
                .screenshots(build1.getScreenshots())
                .schemFile(build1.getSchemFile())
                .build();
        Build savedBuild = createTestBuild(TEST_ID_1, BUILD_NAME_1, build1.getAuthors(),
                build1.getThemes(), build1.getColors());
        savedBuild.setDescription("New Description Same Name");

        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);
        // findByName will find the *same* build, so uniqueness check passes
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.of(build1));
        when(buildRepository.save(any(Build.class))).thenReturn(savedBuild);

        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(BUILD_NAME_1); // Name is unchanged
        assertThat(result.getDescription()).isEqualTo("New Description Same Name"); // Desc updated

        verify(buildServiceSpy).findBuildById(TEST_ID_1);
        verify(buildRepository).findByName(BUILD_NAME_1); // Still checks name uniqueness
        verify(buildRepository).save(buildCaptor.capture());
        assertThat(buildCaptor.getValue().getDescription()).isEqualTo("New Description Same Name");

        // Cache Verifications
        verify(cache, never()).evict(BUILD_CACHE_KEY_NAME_1); // Should NOT evict name cache
        verify(cache).put(BUILD_CACHE_KEY_NAME_1, savedBuild); // Should UPDATE name cache
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(BUILD_CACHE_KEY_ID_1), idPutCaptor.capture()); // Update ID cache
        assertThat(idPutCaptor.getValue().getDescription()).isEqualTo("New Description Same Name");
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("updateBuild: Success, No New Schem - Should update metadata, keep old schem")
    void updateBuild_withoutNewSchemFile_shouldNotUpdateSchem() {
        // Arrange
        byte[] originalSchemBytes = build1.getSchemFile();
        assertThat(originalSchemBytes).isNotNull().isNotEmpty(); // Pre-condition

        Build updatedBuildData = Build.builder()
                .name(TEST_NAME_NEW)
                .authors(build1.getAuthors())
                .themes(build1.getThemes())
                .colors(build1.getColors())
                .description("Desc No Schem Update")
                .screenshots(build1.getScreenshots())
                .schemFile(null) // Explicitly null or empty schem in update data
                .build();
        Build savedBuild = createTestBuild(TEST_ID_1, TEST_NAME_NEW, build1.getAuthors(),
                build1.getThemes(), build1.getColors());
        savedBuild.setDescription("Desc No Schem Update");
        savedBuild.setSchemFile(originalSchemBytes); // Should retain original bytes

        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);
        when(buildRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty());
        when(buildRepository.save(any(Build.class))).thenReturn(savedBuild);

        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);
        assertThat(result.getSchemFile()).isEqualTo(originalSchemBytes); // Verify schem is unchanged

        verify(buildServiceSpy).findBuildById(TEST_ID_1);
        verify(buildRepository).findByName(TEST_NAME_NEW);
        verify(buildRepository).save(buildCaptor.capture());
        assertThat(buildCaptor.getValue().getSchemFile()).isEqualTo(originalSchemBytes); // Verify in saved entity

        // Cache verification (similar to name change case)
        verify(cache).evict(BUILD_CACHE_KEY_NAME_1);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.BUILD, TEST_NAME_NEW),
                savedBuild);
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(BUILD_CACHE_KEY_ID_1), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getSchemFile()).isEqualTo(originalSchemBytes);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("updateBuild: Failure - Should throw exception when name exists for different build")
    void updateBuild_whenNameExistsForDifferentBuild_shouldThrowIllegalArgumentException() {
        // Arrange
        Build updatedBuildData = Build.builder().name(BUILD_NAME_2).build(); // Try to use build2's name
        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);
        when(buildRepository.findByName(BUILD_NAME_2)).thenReturn(Optional.of(build2)); // Name exists for build2

        // Act & Assert
        assertThatThrownBy(() -> buildService.updateBuild(TEST_ID_1, updatedBuildData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(BUILD_NAME_2 + "' " + StringConstants.ALREADY_EXISTS_MESSAGE);

        verify(buildServiceSpy).findBuildById(TEST_ID_1);
        verify(buildRepository).findByName(BUILD_NAME_2);
        verify(buildRepository, never()).save(any());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("updateBuild: Failure - Should throw exception when build to update is not found")
    void updateBuild_whenBuildNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        Build updatedBuildData = Build.builder().name(TEST_NAME_NEW).build();
        doReturn(Optional.empty()).when(buildServiceSpy).findBuildById(NON_EXISTENT_ID);

        // Act & Assert
        assertThatThrownBy(() -> buildService.updateBuild(NON_EXISTENT_ID, updatedBuildData))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_ID + " '"
                        + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(buildServiceSpy).findBuildById(NON_EXISTENT_ID);
        verify(buildRepository, never()).findByName(anyString());
        verify(buildRepository, never()).save(any());
    }

    // --- deleteBuild Tests ---

    @Test
    @DisplayName("deleteBuild: Success - Should delete from repo and evict caches")
    void deleteBuild_whenExists_shouldDeleteFromRepoAndEvictCaches() {
        // Arrange
        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);

        // Act
        buildService.deleteBuild(TEST_ID_1);

        // Assert
        verify(buildServiceSpy).findBuildById(TEST_ID_1);
        verify(buildRepository).deleteById(TEST_ID_1);
        verify(cache).evict(BUILD_CACHE_KEY_ID_1);
        verify(cache).evict(BUILD_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteBuild: Failure - Should throw exception when build not found")
    void deleteBuild_whenNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        doReturn(Optional.empty()).when(buildServiceSpy).findBuildById(NON_EXISTENT_ID);

        // Act & Assert
        assertThatThrownBy(() -> buildService.deleteBuild(NON_EXISTENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_ID + " '"
                        + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(buildServiceSpy).findBuildById(NON_EXISTENT_ID);
        verify(buildRepository, never()).deleteById(anyLong());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    // --- findBuildByIdentifier Tests ---

    @Test
    @DisplayName("findBuildByIdentifier: Success - Should find by ID when identifier is numeric")
    void findBuildByIdentifier_whenNumeric_shouldFindById() {
        // Arrange
        String identifier = String.valueOf(TEST_ID_1);
        // Mock the internal call via the spy
        doReturn(Optional.of(build1)).when(buildServiceSpy).findBuildById(TEST_ID_1);

        // Act
        Build foundBuild = buildService.findBuildByIdentifier(identifier);

        // Assert
        assertThat(foundBuild).isEqualTo(build1);
        verify(buildServiceSpy).findBuildById(TEST_ID_1);
        verify(buildServiceSpy, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findBuildByIdentifier: Success - Should find by name when identifier is not numeric")
    void findBuildByIdentifier_whenNonNumeric_shouldFindByName() {
        // Arrange
        // Mock the internal call via the spy
        doReturn(Optional.of(build1)).when(buildServiceSpy).findByName(BUILD_NAME_1);

        // Act
        Build foundBuild = buildService.findBuildByIdentifier(BUILD_NAME_1);

        // Assert
        assertThat(foundBuild).isEqualTo(build1);
        verify(buildServiceSpy, never()).findBuildById(anyLong());
        verify(buildServiceSpy).findByName(BUILD_NAME_1);
    }

    @Test
    @DisplayName("findBuildByIdentifier: Failure - Should throw exception if ID not found")
    void findBuildByIdentifier_whenNumericNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        String identifier = String.valueOf(NON_EXISTENT_ID);
        doReturn(Optional.empty()).when(buildServiceSpy).findBuildById(NON_EXISTENT_ID);

        // Act & Assert
        assertThatThrownBy(() -> buildService.findBuildByIdentifier(identifier))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_ID + " '"
                        + identifier + "' " + StringConstants.NOT_FOUND_MESSAGE);
        verify(buildServiceSpy).findBuildById(NON_EXISTENT_ID);
        verify(buildServiceSpy, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findBuildByIdentifier: Failure - Should throw exception if name not found")
    void findBuildByIdentifier_whenNonNumericNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        String identifier = TEST_NAME_NON_EXISTENT;
        doReturn(Optional.empty()).when(buildServiceSpy).findByName(identifier);

        // Act & Assert
        assertThatThrownBy(() -> buildService.findBuildByIdentifier(identifier))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_NAME + " '"
                        + identifier + "' " + StringConstants.NOT_FOUND_MESSAGE);
        verify(buildServiceSpy, never()).findBuildById(anyLong());
        verify(buildServiceSpy).findByName(identifier);
    }

    // --- findBuildFullyLoadedForLog Tests ---

    @Test
    @DisplayName("findBuildFullyLoadedForLog: Success - Should call repository method")
    void findBuildFullyLoadedForLog_shouldCallRepository() {
        // Arrange
        when(buildRepository.findByIdWithAssociationsForLog(TEST_ID_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findBuildFullyLoadedForLog(TEST_ID_1);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(buildRepository).findByIdWithAssociationsForLog(TEST_ID_1);
        // Verify cache is NOT interacted with for this specific method (as per current implementation)
        verify(cache, never()).get(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findBuildFullyLoadedForLog: Not Found - Should return empty optional")
    void findBuildFullyLoadedForLog_whenNotFound_shouldReturnEmpty() {
        // Arrange
        when(buildRepository.findByIdWithAssociationsForLog(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<Build> foundBuild = buildService.findBuildFullyLoadedForLog(NON_EXISTENT_ID);

        // Assert
        assertThat(foundBuild).isEmpty();
        verify(buildRepository).findByIdWithAssociationsForLog(NON_EXISTENT_ID);
    }
}