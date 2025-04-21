package sovok.mcbuildlibrary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sovok.mcbuildlibrary.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class BuildServiceTest {

    @Mock
    private BuildRepository buildRepository;
    @Mock
    private InMemoryCache cache;

    @InjectMocks
    private BuildService buildService;

    @Captor
    private ArgumentCaptor<Build> buildCaptor;
    @Captor

    private Build build1;
    private Build build2;


    @BeforeEach
    void setUp() {
        Author author1 = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);
        Theme theme1 = createTestTheme(TEST_ID_1, THEME_NAME_1);
        Color color1 = createTestColor(TEST_ID_1, COLOR_NAME_1);

        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(author1), Set.of(theme1), Set.of(color1));
        build2 = createTestBuild(TEST_ID_2, BUILD_NAME_2, Set.of(author1), Set.of(theme1), Set.of(color1));
    }

    // --- Existing Tests (createBuild, findById, findAll, some findByName, filterBuilds, some getSchemFile, some update, delete) ---
    // ... (Keep all previously passing tests here) ...

    @Test
    @DisplayName("createBuild_whenNameDoesNotExist_shouldSaveAndCache")
    void createBuild_whenNameDoesNotExist_shouldSaveAndCache() {
        // Arrange
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.empty());
        when(buildRepository.save(any(Build.class))).thenReturn(build1); // Return build with ID

        // Act
        Build createdBuild = buildService.createBuild(build1); // Pass build without ID

        // Assert
        assertThat(createdBuild).isNotNull();
        assertThat(createdBuild.getId()).isEqualTo(TEST_ID_1);
        assertThat(createdBuild.getName()).isEqualTo(BUILD_NAME_1);

        verify(buildRepository).findByName(BUILD_NAME_1);
        verify(buildRepository).save(buildCaptor.capture());
        assertThat(buildCaptor.getValue().getName()).isEqualTo(BUILD_NAME_1);
        // Check data passed to save

        verify(cache).put(BUILD_CACHE_KEY_ID_1, createdBuild));
        verify(cache).put(BUILD_CACHE_KEY_NAME_1, createdBuild);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("createBuild_whenNameExists_shouldThrowIllegalArgumentException")
    void createBuild_whenNameExists_shouldThrowIllegalArgumentException() {
        // Arrange
        Build buildWithNoId = createTestBuild(null, BUILD_NAME_1, Set.of(), Set.of(),
                Set.of()); // Build data to be created
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.of(build1));
        // Mock existing build found

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

    @Test
    @DisplayName("findBuildById_whenCached_shouldReturnCached")
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
    @DisplayName("findBuildById_whenNotCachedAndExists_shouldFetchAndCache")
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
    @DisplayName("findBuildById_whenNotFound_shouldReturnEmpty") // Added for completeness
    void findBuildById_whenNotFound_shouldReturnEmpty() {
        // Arrange
        when(cache.get(InMemoryCache.generateKey(StringConstants.BUILD, NON_EXISTENT_ID)))
                .thenReturn(Optional.empty());
        when(buildRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<Build> foundBuild = buildService.findBuildById(NON_EXISTENT_ID);

        // Assert
        assertThat(foundBuild).isEmpty();
        verify(cache).get(InMemoryCache.generateKey(StringConstants.BUILD, NON_EXISTENT_ID));
        verify(buildRepository).findById(NON_EXISTENT_ID);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findByName_whenCachedAndMatches_shouldReturnCached") // Added test
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
    @DisplayName("findByName_whenNotCachedAndExists_shouldFetchAndCache")
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
    @DisplayName("findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo") // New Test
    void findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo() {
        // Arrange
        String requestedName = "buildone"; // Requesting lowercase name
        String cacheKey = InMemoryCache.generateKey(StringConstants.BUILD, requestedName);
        // Assume cache somehow has the correct ID but wrong casing for the name key
        when(cache.get(cacheKey)).thenReturn(Optional.of(build1)); // build1 has name "BuildOne"
        // Repo returns the correct build for the requested name (assuming case-insensitive repo or exact match)
        when(buildRepository.findByName(requestedName)).thenReturn(Optional.of(build1));

        // Act
        Optional<Build> foundBuild = buildService.findByName(requestedName);

        // Assert
        assertThat(foundBuild).isPresent().contains(build1);
        verify(cache).get(cacheKey);
        // Verify cache is evicted because cached name "BuildOne" != requested name "buildone"
        verify(cache).evict(cacheKey);
        verify(buildRepository).findByName(requestedName);
        // Verify cache is updated with the correct key-value pair
        verify(cache).put(eq(cacheKey), eq(build1));
    }

    @Test
    @DisplayName("findByName_whenNotFound_shouldReturnEmpty") // New Test
    void findByName_whenNotFound_shouldReturnEmpty() {
        // Arrange
        String nonExistentName = TEST_NAME_NON_EXISTENT;
        String cacheKey = InMemoryCache.generateKey(StringConstants.BUILD, nonExistentName);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findByName(nonExistentName)).thenReturn(Optional.empty());

        // Act
        Optional<Build> foundBuild = buildService.findByName(nonExistentName);

        // Assert
        assertThat(foundBuild).isEmpty();
        verify(cache).get(cacheKey);
        verify(buildRepository).findByName(nonExistentName);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findAll_shouldReturnFromRepositoryDirectly")
    void findAll_shouldReturnFromRepositoryDirectly() {
        // Arrange
        List<Build> expectedBuilds = List.of(build1, build2);
        when(buildRepository.findAll()).thenReturn(expectedBuilds);

        // Act
        List<Build> actualBuilds = buildService.findAll();

        // Assert
        assertThat(actualBuilds).isEqualTo(expectedBuilds);
        verify(buildRepository).findAll();
        verify(cache, never()).get(anyString());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("filterBuilds_whenNotCached_shouldQueryRepoAndCache")
    void filterBuilds_whenNotCached_shouldQueryRepoAndCache() {
        // Arrange
        String authorQuery = "Auth";
        String nameQuery = null;
        String themeQuery = "Theme";
        String colorQuery = null;
        List<Build> repoResult = List.of(build1);

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", nameQuery);
        params.put("theme", themeQuery);
        params.put("color", colorQuery);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);


        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(buildRepository.fuzzyFilterBuilds(authorQuery, nameQuery, themeQuery, colorQuery)).thenReturn(repoResult);


        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, nameQuery, themeQuery, colorQuery);


        // Assert
        assertThat(filteredBuilds).isEqualTo(repoResult);
        verify(cache).get(queryKey);
        verify(buildRepository).fuzzyFilterBuilds(authorQuery, nameQuery, themeQuery, colorQuery);
        verify(cache).put(queryKey, repoResult);
    }

    @Test
    @DisplayName("filterBuilds_whenCached_shouldReturnCachedResult")
    void filterBuilds_whenCached_shouldReturnCachedResult() {
        // Arrange
        String authorQuery = "Auth";
        String nameQuery = null;
        String themeQuery = "Theme";
        String colorQuery = null;
        List<Build> cachedResult = List.of(build1, build2);

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", nameQuery);
        params.put("theme", themeQuery);
        params.put("color", colorQuery);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);

        when(cache.get(queryKey)).thenReturn(Optional.of(cachedResult));

        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, nameQuery, themeQuery, colorQuery);

        // Assert
        assertThat(filteredBuilds).isEqualTo(cachedResult);
        verify(cache).get(queryKey);
        verify(buildRepository, never()).fuzzyFilterBuilds(any(), any(), any(), any());
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("filterBuilds_whenReturnsEmptyList_shouldCacheEmptyList") // New Test
    void filterBuilds_whenReturnsEmptyList_shouldCacheEmptyList() {
        // Arrange
        String authorQuery = "NonExistentAuthor";
        String nameQuery = "NonExistentName";
        String themeQuery = null;
        String colorQuery = null;
        List<Build> repoResult = Collections.emptyList();

        Map<String, Object> params = new HashMap<>();
        params.put("author", authorQuery);
        params.put("name", nameQuery);
        params.put("theme", themeQuery);
        params.put("color", colorQuery);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.BUILD, params);

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(buildRepository.fuzzyFilterBuilds(authorQuery, nameQuery, themeQuery, colorQuery)).thenReturn(repoResult);

        // Act
        List<Build> filteredBuilds = buildService.filterBuilds(authorQuery, nameQuery, themeQuery, colorQuery);

        // Assert
        assertThat(filteredBuilds).isEmpty();
        verify(cache).get(queryKey);
        verify(buildRepository).fuzzyFilterBuilds(authorQuery, nameQuery, themeQuery, colorQuery);
        // IMPORTANT: Verify that even an empty list gets cached to prevent repeated repo calls for the same query
        verify(cache).put(queryKey, repoResult);
    }


    @Test
    @DisplayName("getSchemFile_whenBuildExistsAndHasFile_shouldReturnBytes")
    void getSchemFile_whenBuildExistsAndHasFile_shouldReturnBytes() {
        // Arrange
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isPresent().contains(TEST_SCHEM_BYTES);
        verify(buildRepository).findById(TEST_ID_1);
        verify(cache).put(BUILD_CACHE_KEY_ID_1, build1);
    }

    @Test
    @DisplayName("getSchemFile_whenBuildExistsButNoFile_shouldReturnEmpty")
    void getSchemFile_whenBuildExistsButNoFile_shouldReturnEmpty() {
        // Arrange
        Build buildWithoutSchem = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(), Set.of(), Set.of());
        buildWithoutSchem.setSchemFile(null);
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(buildWithoutSchem));

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isEmpty();
        verify(buildRepository).findById(TEST_ID_1);
        verify(cache).put(BUILD_CACHE_KEY_ID_1, buildWithoutSchem);
    }

    @Test
    @DisplayName("getSchemFile_whenBuildExistsButEmptyFile_shouldReturnEmpty") // New Test
    void getSchemFile_whenBuildExistsButEmptyFile_shouldReturnEmpty() {
        // Arrange
        Build buildWithEmptySchem = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(), Set.of(), Set.of());
        buildWithEmptySchem.setSchemFile(new byte[0]); // Empty byte array
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(buildWithEmptySchem));

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(TEST_ID_1);

        // Assert
        assertThat(schemBytes).isEmpty(); // Should be empty due to the filter
        verify(buildRepository).findById(TEST_ID_1);
        verify(cache).put(BUILD_CACHE_KEY_ID_1, buildWithEmptySchem);
    }

    @Test
    @DisplayName("getSchemFile_whenBuildNotFound_shouldReturnEmpty") // New Test
    void getSchemFile_whenBuildNotFound_shouldReturnEmpty() {
        // Arrange
        when(cache.get(InMemoryCache.generateKey(StringConstants.BUILD, NON_EXISTENT_ID))).thenReturn(Optional.empty());
        when(buildRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act
        Optional<byte[]> schemBytes = buildService.getSchemFile(NON_EXISTENT_ID);

        // Assert
        assertThat(schemBytes).isEmpty();
        verify(buildRepository).findById(NON_EXISTENT_ID);
        verify(cache, never()).put(anyString(), any()); // Nothing to cache
    }


    @Test
    @DisplayName("updateBuild_whenExistsAndNameUnique_shouldUpdateAndManageCache")
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
        Build savedBuild = createTestBuild(TEST_ID_1, TEST_NAME_NEW, build1.getAuthors(), build1.getThemes(), build1.getColors());
        savedBuild.setDescription("Updated Desc");
        savedBuild.setScreenshots(List.of("new.png"));
        savedBuild.setSchemFile("new schem".getBytes());
        String oldNameCacheKey = BUILD_CACHE_KEY_NAME_1;
        String newNameCacheKey = InMemoryCache.generateKey(StringConstants.BUILD, TEST_NAME_NEW);
        String idCacheKey = BUILD_CACHE_KEY_ID_1;

        when(cache.get(idCacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));
        when(buildRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty());
        when(buildRepository.save(any(Build.class))).thenReturn(savedBuild);

        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);
        assertThat(result.getDescription()).isEqualTo("Updated Desc");

        verify(buildRepository).findById(TEST_ID_1);
        verify(buildRepository).findByName(TEST_NAME_NEW);
        verify(buildRepository).save(buildCaptor.capture());

        // Cache Verifications
        verify(cache).evict(eq(oldNameCacheKey));
        verify(cache).put(eq(newNameCacheKey), eq(savedBuild));
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(idCacheKey), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("updateBuild_whenNameDoesNotChange_shouldUpdateAndManageCache") // New Test
    void updateBuild_whenNameDoesNotChange_shouldUpdateAndManageCache() {
        // Arrange
        // Update data with the SAME name but different description
        Build updatedBuildData = Build.builder()
                .name(BUILD_NAME_1) // SAME name
                .authors(build1.getAuthors())
                .themes(build1.getThemes())
                .colors(build1.getColors())
                .description("New Description Same Name") // Different description
                .screenshots(build1.getScreenshots())
                .schemFile(build1.getSchemFile())
                .build();

        // Mock the final saved state
        Build savedBuild = createTestBuild(TEST_ID_1, BUILD_NAME_1, build1.getAuthors(), build1.getThemes(), build1.getColors());
        savedBuild.setDescription("New Description Same Name");

        String nameCacheKey = BUILD_CACHE_KEY_NAME_1;
        String idCacheKey = BUILD_CACHE_KEY_ID_1;

        when(cache.get(idCacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));
        // findByName should be called, but it will find the *current* build (build1), so the check passes
        when(buildRepository.findByName(BUILD_NAME_1)).thenReturn(Optional.of(build1));
        when(buildRepository.save(any(Build.class))).thenReturn(savedBuild);

        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(BUILD_NAME_1); // Name is unchanged
        assertThat(result.getDescription()).isEqualTo("New Description Same Name"); // Description updated

        verify(buildRepository).findById(TEST_ID_1);
        verify(buildRepository).findByName(BUILD_NAME_1); // Still checks name uniqueness
        verify(buildRepository).save(buildCaptor.capture());
        assertThat(buildCaptor.getValue().getDescription()).isEqualTo("New Description Same Name");

        // Cache Verifications
        verify(cache, never()).evict(eq(nameCacheKey)); // Should NOT evict name cache if name didn't change
        // Should update the name cache with the new data, even if name is the same
        verify(cache).put(eq(nameCacheKey), eq(savedBuild));
        // Should update the ID cache (at least once)
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(idCacheKey), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getDescription()).isEqualTo("New Description Same Name"); // Check it has updated data
        // Should evict query cache
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("updateBuild_withoutNewSchemFile_shouldNotUpdateSchem") // New Test
    void updateBuild_withoutNewSchemFile_shouldNotUpdateSchem() {
        // Arrange
        byte[] originalSchemBytes = build1.getSchemFile(); // Store original bytes
        assertThat(originalSchemBytes).isNotNull().isNotEmpty(); // Pre-condition check

        // Update data with NO schem file provided
        Build updatedBuildData = Build.builder()
                .name(TEST_NAME_NEW)
                .authors(build1.getAuthors())
                .themes(build1.getThemes())
                .colors(build1.getColors())
                .description("Desc No Schem Update")
                .screenshots(build1.getScreenshots())
                .schemFile(null) // Explicitly null schem
                .build();

        // Mock the final saved state - schem file should be the original one
        Build savedBuild = createTestBuild(TEST_ID_1, TEST_NAME_NEW, build1.getAuthors(), build1.getThemes(), build1.getColors());
        savedBuild.setDescription("Desc No Schem Update");
        savedBuild.setSchemFile(originalSchemBytes); // Should still have the original bytes

        String idCacheKey = BUILD_CACHE_KEY_ID_1;

        when(cache.get(idCacheKey)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));
        when(buildRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty());
        // Mock save, importantly ensuring the passed build still has the original schem
        when(buildRepository.save(any(Build.class))).thenAnswer(invocation -> {
            Build buildToSave = invocation.getArgument(0);
            // Simulate JPA behavior - if field wasn't set in update, it keeps old value
            assertThat(buildToSave.getSchemFile()).isEqualTo(originalSchemBytes);
            // Return the 'saved' state which should also have the original schem
            return savedBuild;
        });


        // Act
        Build result = buildService.updateBuild(TEST_ID_1, updatedBuildData);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);
        assertThat(result.getDescription()).isEqualTo("Desc No Schem Update");
        // Assert that the schem file in the result is the original one
        assertThat(result.getSchemFile()).isEqualTo(originalSchemBytes);

        verify(buildRepository).findById(TEST_ID_1);
        verify(buildRepository).findByName(TEST_NAME_NEW);
        verify(buildRepository).save(buildCaptor.capture());
        // Assert the build passed to save still had the original schem bytes
        assertThat(buildCaptor.getValue().getSchemFile()).isEqualTo(originalSchemBytes);

        // Cache verification (ensure updated build with original schem is cached)
        verify(cache).evict(BUILD_CACHE_KEY_NAME_1); // Old name evicted
        verify(cache).put(InMemoryCache.generateKey(StringConstants.BUILD, TEST_NAME_NEW), savedBuild); // New name cached
        ArgumentCaptor<Build> idPutCaptor = ArgumentCaptor.forClass(Build.class);
        verify(cache, atLeastOnce()).put(eq(idCacheKey), idPutCaptor.capture());
        assertThat(idPutCaptor.getValue().getSchemFile()).isEqualTo(originalSchemBytes); // Check schem in cached object
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }


    @Test
    @DisplayName("updateBuild_whenNameExistsForDifferentBuild_shouldThrowIllegalArgumentException")
    void updateBuild_whenNameExistsForDifferentBuild_shouldThrowIllegalArgumentException() {
        // Arrange
        Build updatedBuildData = Build.builder().name(BUILD_NAME_2).build();
        when(cache.get(BUILD_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));
        when(buildRepository.findByName(BUILD_NAME_2)).thenReturn(Optional.of(build2));

        // Act & Assert
        assertThatThrownBy(() -> buildService.updateBuild(TEST_ID_1, updatedBuildData))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(BUILD_NAME_2 + "' " + StringConstants.ALREADY_EXISTS_MESSAGE);

        verify(buildRepository).findById(TEST_ID_1);
        verify(cache).put(BUILD_CACHE_KEY_ID_1, build1);
        verify(buildRepository).findByName(BUILD_NAME_2);
        verify(buildRepository, never()).save(any());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("updateBuild_whenBuildNotFound_shouldThrowNoSuchElementException")
    void updateBuild_whenBuildNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        Build updatedBuildData = Build.builder().name(TEST_NAME_NEW).build();
        when(cache.get(InMemoryCache.generateKey(StringConstants.BUILD, NON_EXISTENT_ID))).thenReturn(Optional.empty());
        when(buildRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> buildService.updateBuild(NON_EXISTENT_ID, updatedBuildData))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(buildRepository).findById(NON_EXISTENT_ID);
        verify(buildRepository, never()).findByName(anyString());
        verify(buildRepository, never()).save(any());
    }


    @Test
    @DisplayName("deleteBuild_whenExists_shouldDeleteFromRepoAndEvictCaches")
    void deleteBuild_whenExists_shouldDeleteFromRepoAndEvictCaches() {
        // Arrange
        when(buildRepository.findById(TEST_ID_1)).thenReturn(Optional.of(build1));

        // Act
        buildService.deleteBuild(TEST_ID_1);

        // Assert
        verify(buildRepository).findById(TEST_ID_1);
        verify(buildRepository).deleteById(TEST_ID_1);
        verify(cache).evict(BUILD_CACHE_KEY_ID_1);
        verify(cache).evict(BUILD_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteBuild_whenNotFound_shouldThrowNoSuchElementException")
    void deleteBuild_whenNotFound_shouldThrowNoSuchElementException() {
        // Arrange
        when(buildRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> buildService.deleteBuild(NON_EXISTENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.BUILD + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(buildRepository).findById(NON_EXISTENT_ID);
        verify(buildRepository, never()).deleteById(anyLong());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }
}