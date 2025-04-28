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
import sovok.mcbuildlibrary.dto.ThemeDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Theme;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ThemeRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sovok.mcbuildlibrary.TestConstants.*;


@ExtendWith(MockitoExtension.class)
class ThemeServiceTest {

    @Mock
    private ThemeRepository themeRepository;
    @Mock
    private BuildRepository buildRepository;
    @Mock
    private InMemoryCache cache;

    @InjectMocks
    private ThemeService themeService;

    @Captor
    private ArgumentCaptor<Theme> themeCaptor;
    @Captor
    private ArgumentCaptor<List<Theme>> themeListCaptor;

    private Theme theme1;
    private Theme theme2;
    private Theme theme3;
    private Build build1;

    @BeforeEach
    void setUp() {
        theme1 = createTestTheme(TEST_ID_1, THEME_NAME_1);
        theme2 = createTestTheme(TEST_ID_2, THEME_NAME_2);
        theme3 = createTestTheme(TEST_ID_3, THEME_NAME_3);
        Author authorStub = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);

        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(authorStub), Set.of(theme1), Set.of());
    }



    @Test
    @DisplayName("create_whenNameDoesNotExist_shouldSaveAndCacheTheme")
    void create_whenNameDoesNotExist_shouldSaveAndCacheTheme() {

        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenReturn(theme1);


        Theme createdTheme = themeService.create(THEME_NAME_1);


        assertThat(createdTheme).isNotNull();
        assertThat(createdTheme.getName()).isEqualTo(THEME_NAME_1);
        assertThat(createdTheme.getId()).isEqualTo(TEST_ID_1);

        verify(themeRepository).findByName(THEME_NAME_1);
        verify(themeRepository).save(themeCaptor.capture());
        assertThat(themeCaptor.getValue().getName()).isEqualTo(THEME_NAME_1);
        assertThat(themeCaptor.getValue().getId()).isNull();

        verify(cache).put(THEME_CACHE_KEY_ID_1, createdTheme);
        verify(cache).put(THEME_CACHE_KEY_NAME_1, createdTheme);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("create_whenNameExists_shouldThrowIllegalArgumentException")
    void create_whenNameExists_shouldThrowIllegalArgumentException() {

        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));


        assertThatThrownBy(() -> themeService.create(THEME_NAME_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format(StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        StringConstants.THEME, StringConstants.WITH_NAME, THEME_NAME_1, StringConstants.ALREADY_EXISTS_MESSAGE));

        verify(themeRepository).findByName(THEME_NAME_1);
        verify(themeRepository, never()).save(any());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }


    @Test
    @DisplayName("findById_whenCached_shouldReturnCachedTheme")
    void findById_whenCached_shouldReturnCachedTheme() {

        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.of(theme1));


        Optional<Theme> foundTheme = themeService.findById(TEST_ID_1);


        assertThat(foundTheme).isPresent().contains(theme1);
        verify(cache).get(THEME_CACHE_KEY_ID_1);
        verify(themeRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("findById_whenNotCachedButExists_shouldFetchAndCache")
    void findById_whenNotCachedButExists_shouldFetchAndCache() {

        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));


        Optional<Theme> foundTheme = themeService.findById(TEST_ID_1);


        assertThat(foundTheme).isPresent().contains(theme1);
        verify(cache).get(THEME_CACHE_KEY_ID_1);
        verify(themeRepository).findById(TEST_ID_1);
        verify(cache).put(THEME_CACHE_KEY_ID_1, theme1);
    }

    @Test
    @DisplayName("findById_whenNotFound_shouldReturnEmpty")
    void findById_whenNotFound_shouldReturnEmpty() {

        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, NON_EXISTENT_ID))).thenReturn(Optional.empty());
        when(themeRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());


        Optional<Theme> foundTheme = themeService.findById(NON_EXISTENT_ID);


        assertThat(foundTheme).isEmpty();
        verify(cache).get(InMemoryCache.generateKey(StringConstants.THEME, NON_EXISTENT_ID));
        verify(themeRepository).findById(NON_EXISTENT_ID);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findByName_whenCachedAndMatches_shouldReturnCached")
    void findByName_whenCachedAndMatches_shouldReturnCached() {

        when(cache.get(THEME_CACHE_KEY_NAME_1)).thenReturn(Optional.of(theme1));


        Optional<Theme> foundTheme = themeService.findByName(THEME_NAME_1);


        assertThat(foundTheme).isPresent().contains(theme1);
        verify(cache).get(THEME_CACHE_KEY_NAME_1);
        verify(cache, never()).evict(anyString());
        verify(themeRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo")
    void findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo() {

        String requestedNameLower = THEME_NAME_1.toLowerCase();
        String cacheKey = InMemoryCache.generateKey(StringConstants.THEME, requestedNameLower);
        when(cache.get(cacheKey)).thenReturn(Optional.of(theme1));
        when(themeRepository.findByName(requestedNameLower)).thenReturn(Optional.of(theme1));


        Optional<Theme> foundTheme = themeService.findByName(requestedNameLower);


        assertThat(foundTheme).isPresent().contains(theme1);
        verify(cache).get(cacheKey);
        verify(cache).evict(cacheKey);
        verify(themeRepository).findByName(requestedNameLower);
        verify(cache).put(cacheKey, theme1);
    }

    @Test
    @DisplayName("findByName_whenNotCachedButExists_shouldFetchAndCache")
    void findByName_whenNotCachedButExists_shouldFetchAndCache() {

        when(cache.get(THEME_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());
        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));


        Optional<Theme> foundTheme = themeService.findByName(THEME_NAME_1);


        assertThat(foundTheme).isPresent().contains(theme1);
        verify(cache).get(THEME_CACHE_KEY_NAME_1);
        verify(themeRepository).findByName(THEME_NAME_1);
        verify(cache).put(THEME_CACHE_KEY_NAME_1, theme1);
    }

    @Test
    @DisplayName("findByName_whenNotFound_shouldReturnEmpty")
    void findByName_whenNotFound_shouldReturnEmpty() {

        String nonExistentName = TEST_NAME_NON_EXISTENT;
        String cacheKey = InMemoryCache.generateKey(StringConstants.THEME, nonExistentName);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(themeRepository.findByName(nonExistentName)).thenReturn(Optional.empty());


        Optional<Theme> foundTheme = themeService.findByName(nonExistentName);


        assertThat(foundTheme).isEmpty();
        verify(cache).get(cacheKey);
        verify(themeRepository).findByName(nonExistentName);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findDtoById_whenExists_shouldFetchAndConvertToDto")
    void findDtoById_whenExists_shouldFetchAndConvertToDto() {

        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_1)).thenReturn(relatedBuilds);


        Optional<ThemeDto> foundDto = themeService.findDtoById(TEST_ID_1);


        assertThat(foundDto).isPresent();
        assertThat(foundDto.get().id()).isEqualTo(TEST_ID_1);
        assertThat(foundDto.get().name()).isEqualTo(THEME_NAME_1);
        assertThat(foundDto.get().relatedBuilds()).hasSize(1);
        assertThat(foundDto.get().relatedBuilds().get(0).id()).isEqualTo(build1.getId());
        assertThat(foundDto.get().relatedBuilds().get(0).name()).isEqualTo(build1.getName());

        verify(themeRepository).findById(TEST_ID_1);
        verify(cache).put(THEME_CACHE_KEY_ID_1, theme1);
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtoById_whenNotFound_shouldReturnEmpty")
    void findDtoById_whenNotFound_shouldReturnEmpty() {

        when(themeRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        Optional<ThemeDto> foundDto = themeService.findDtoById(NON_EXISTENT_ID);


        assertThat(foundDto).isEmpty();
        verify(themeRepository).findById(NON_EXISTENT_ID);
        verify(buildRepository, never()).findBuildIdAndNameByThemeId(anyLong());
    }


    @Test
    @DisplayName("findAllDtos_shouldFetchAllFromRepoAndConvert")
    void findAllDtos_shouldFetchAllFromRepoAndConvert() {

        List<Theme> themes = List.of(theme1, theme2);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = Collections.emptyList();

        when(themeRepository.findAll()).thenReturn(themes);
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_2)).thenReturn(relatedBuilds2);


        List<ThemeDto> allDtos = themeService.findAllDtos();


        assertThat(allDtos).hasSize(2);
        assertThat(allDtos.get(0).id()).isEqualTo(TEST_ID_1);
        assertThat(allDtos.get(0).name()).isEqualTo(THEME_NAME_1);
        assertThat(allDtos.get(0).relatedBuilds()).hasSize(1);
        assertThat(allDtos.get(1).id()).isEqualTo(TEST_ID_2);
        assertThat(allDtos.get(1).name()).isEqualTo(THEME_NAME_2);
        assertThat(allDtos.get(1).relatedBuilds()).isEmpty();

        verify(themeRepository).findAll();
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_2);
    }

    @Test
    @DisplayName("findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert")
    void findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert() {

        String query = "Theme";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.THEME, params);
        List<Theme> repoResult = List.of(theme1);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(themeRepository.fuzzyFindByName(query)).thenReturn(repoResult);
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_1)).thenReturn(relatedBuilds1);


        List<ThemeDto> queryDtos = themeService.findDtosByNameQuery(query);


        assertThat(queryDtos).hasSize(1);
        assertThat(queryDtos.get(0).name()).isEqualTo(THEME_NAME_1);

        verify(cache).get(queryKey);
        verify(themeRepository).fuzzyFindByName(query);
        verify(cache).put(queryKey, repoResult);
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert")
    void findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert() {

        String query = "Theme";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.THEME, params);
        List<Theme> cachedRepoResult = List.of(theme1);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.of(cachedRepoResult));
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_1)).thenReturn(relatedBuilds1);


        List<ThemeDto> queryDtos = themeService.findDtosByNameQuery(query);


        assertThat(queryDtos).hasSize(1);
        assertThat(queryDtos.get(0).name()).isEqualTo(THEME_NAME_1);

        verify(cache).get(queryKey);
        verify(themeRepository, never()).fuzzyFindByName(any());
        verify(cache, never()).put(anyString(), any());
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery")
    void findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery() {

        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, "__NULL__");
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.THEME, params);
        List<Theme> repoResult = List.of(theme1, theme2);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(themeRepository.fuzzyFindByName(null)).thenReturn(repoResult);
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByThemeId(TEST_ID_2)).thenReturn(relatedBuilds2);


        List<ThemeDto> queryDtos = themeService.findDtosByNameQuery(null);


        assertThat(queryDtos).hasSize(2);
        verify(cache).get(queryKey);
        verify(themeRepository).fuzzyFindByName(null);
        verify(cache).put(queryKey, repoResult);
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByThemeId(TEST_ID_2);
    }

    @Test
    @DisplayName("update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries")
    void update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries() {

        Theme updatedTheme = createTestTheme(TEST_ID_1, TEST_NAME_NEW);
        String newNameCacheKey = InMemoryCache.generateKey(StringConstants.THEME, TEST_NAME_NEW);

        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(themeRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenReturn(updatedTheme);
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        Theme result = themeService.update(TEST_ID_1, TEST_NAME_NEW);


        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);

        verify(themeRepository).findById(TEST_ID_1);
        verify(themeRepository).findByName(TEST_NAME_NEW);
        verify(themeRepository).save(themeCaptor.capture());
        assertThat(themeCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW);

        verify(cache).evict(THEME_CACHE_KEY_NAME_1);
        verify(cache).put(newNameCacheKey, updatedTheme);
        verify(cache, atLeastOnce()).put(eq(THEME_CACHE_KEY_ID_1), any(Theme.class));
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName")
    void update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName() {

        Theme savedTheme = createTestTheme(TEST_ID_1, THEME_NAME_1);

        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));
        when(themeRepository.save(any(Theme.class))).thenReturn(savedTheme);
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        Theme result = themeService.update(TEST_ID_1, THEME_NAME_1);


        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(THEME_NAME_1);

        verify(themeRepository).findById(TEST_ID_1);
        verify(themeRepository).findByName(THEME_NAME_1);
        verify(themeRepository).save(any(Theme.class));

        verify(cache, never()).evict(THEME_CACHE_KEY_NAME_1);
        verify(cache).put(THEME_CACHE_KEY_NAME_1, savedTheme);
        verify(cache, atLeastOnce()).put(eq(THEME_CACHE_KEY_ID_1), any(Theme.class));
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException")
    void update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException() {

        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(themeRepository.findByName(THEME_NAME_2)).thenReturn(Optional.of(theme2));
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.update(TEST_ID_1, THEME_NAME_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(THEME_NAME_2 + "' " + StringConstants.ALREADY_EXISTS_MESSAGE);

        verify(themeRepository).findById(TEST_ID_1);
        verify(themeRepository).findByName(THEME_NAME_2);
        verify(themeRepository, never()).save(any());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("update_whenNotFound_shouldThrowNoSuchElementException")
    void update_whenNotFound_shouldThrowNoSuchElementException() {

        when(themeRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.update(NON_EXISTENT_ID, TEST_NAME_NEW))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.THEME + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(themeRepository).findById(NON_EXISTENT_ID);
        verify(themeRepository, never()).findByName(anyString());
        verify(themeRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById_whenThemeIsAssociatedWithBuilds_shouldThrowIllegalStateException")
    void deleteById_whenThemeIsAssociatedWithBuilds_shouldThrowIllegalStateException() {

        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(TEST_ID_1)).thenReturn(List.of(build1));
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.deleteById(TEST_ID_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.THEME, theme1.getName(), 1));

        verify(themeRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByThemeId(TEST_ID_1);
        verify(themeRepository, never()).delete(any());
        verify(cache, never()).evict(anyString());
    }

    @Test
    @DisplayName("deleteById_whenThemeIsNotAssociated_shouldDeleteAndEvictCaches")
    void deleteById_whenThemeIsNotAssociated_shouldDeleteAndEvictCaches() {

        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(TEST_ID_1)).thenReturn(Collections.emptyList());
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        themeService.deleteById(TEST_ID_1);


        verify(themeRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByThemeId(TEST_ID_1);
        verify(themeRepository).delete(theme1);


        verify(cache).evict(THEME_CACHE_KEY_ID_1);
        verify(cache).evict(THEME_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
        verify(cache, never()).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteById_whenNotFound_shouldThrowNoSuchElementException")
    void deleteById_whenNotFound_shouldThrowNoSuchElementException() {

        when(themeRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.deleteById(NON_EXISTENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.THEME + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(themeRepository).findById(NON_EXISTENT_ID);
        verify(themeRepository, never()).delete(any());
        verify(buildRepository, never()).findBuildsByThemeId(anyLong());
        verify(cache, never()).evict(anyString());
    }

    @Test
    @DisplayName("deleteByName_whenExistsAndNotAssociated_shouldDeleteAndEvict")
    void deleteByName_whenExistsAndNotAssociated_shouldDeleteAndEvict() {

        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(theme1.getId())).thenReturn(Collections.emptyList());
        when(cache.get(THEME_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        themeService.deleteByName(THEME_NAME_1);


        verify(themeRepository).findByName(THEME_NAME_1);
        verify(buildRepository).findBuildsByThemeId(theme1.getId());
        verify(themeRepository).delete(theme1);
        verify(cache).evict(THEME_CACHE_KEY_ID_1);
        verify(cache).evict(THEME_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("deleteByName_whenExistsAndAssociated_shouldThrowIllegalStateException")
    void deleteByName_whenExistsAndAssociated_shouldThrowIllegalStateException() {

        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(theme1.getId())).thenReturn(List.of(build1));
        when(cache.get(THEME_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.deleteByName(THEME_NAME_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.THEME, theme1.getName(), 1));

        verify(themeRepository).findByName(THEME_NAME_1);
        verify(buildRepository).findBuildsByThemeId(theme1.getId());
        verify(themeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteByName_whenNotFound_shouldThrowNoSuchElementException")
    void deleteByName_whenNotFound_shouldThrowNoSuchElementException() {

        when(themeRepository.findByName(TEST_NAME_NON_EXISTENT)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, TEST_NAME_NON_EXISTENT))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> themeService.deleteByName(TEST_NAME_NON_EXISTENT))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.THEME + " " + StringConstants.WITH_NAME + " '" + TEST_NAME_NON_EXISTENT + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(themeRepository).findByName(TEST_NAME_NON_EXISTENT);
        verify(themeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("findOrCreate_whenExists_shouldReturnExisting")
    void findOrCreate_whenExists_shouldReturnExisting() {

        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.of(theme1));
        when(cache.get(THEME_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        Theme result = themeService.findOrCreate(THEME_NAME_1);


        assertThat(result).isEqualTo(theme1);
        verify(themeRepository).findByName(THEME_NAME_1);
        verify(themeRepository, never()).save(any());
        verify(cache).put(THEME_CACHE_KEY_NAME_1, theme1);
    }

    @Test
    @DisplayName("findOrCreate_whenNotExists_shouldCreateAndReturnNew")
    void findOrCreate_whenNotExists_shouldCreateAndReturnNew() {

        String newName = "NewThemeToCreate";
        Theme newlyCreatedTheme = createTestTheme(TEST_ID_3, newName);

        when(themeRepository.findByName(newName)).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenReturn(newlyCreatedTheme);
        when(cache.get(InMemoryCache.generateKey(StringConstants.THEME, newName))).thenReturn(Optional.empty());


        Theme result = themeService.findOrCreate(newName);


        assertThat(result).isEqualTo(newlyCreatedTheme);

        verify(themeRepository, times(2)).findByName(newName);
        verify(themeRepository).save(themeCaptor.capture());
        assertThat(themeCaptor.getValue().getName()).isEqualTo(newName);

        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, newlyCreatedTheme.getId()), newlyCreatedTheme);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, newName), newlyCreatedTheme);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("createBulk_whenSomeExist_shouldCreateNewAndSkipExisting")
    void createBulk_whenSomeExist_shouldCreateNewAndSkipExisting() {

        List<String> namesToCreate = List.of(THEME_NAME_1, THEME_NAME_2, THEME_NAME_3, "  " + THEME_NAME_1 + "  ");
        Set<String> uniqueLowerNames = Set.of(THEME_NAME_1.toLowerCase(), THEME_NAME_2.toLowerCase(), THEME_NAME_3.toLowerCase());
        Set<Theme> existingThemes = Set.of(theme1);
        List<Theme> savedEntities = List.of(theme2, theme3);

        when(themeRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingThemes);
        when(themeRepository.saveAll(anyList())).thenReturn(savedEntities);


        BulkCreationResult<String> result = themeService.createBulk(namesToCreate);


        assertThat(result.createdItems()).containsExactlyInAnyOrder(THEME_NAME_2, THEME_NAME_3);
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(THEME_NAME_1);

        verify(themeRepository).findByNamesIgnoreCase(uniqueLowerNames);
        verify(themeRepository).saveAll(themeListCaptor.capture());
        List<Theme> themesPassedToSaveAll = themeListCaptor.getValue();
        assertThat(themesPassedToSaveAll).hasSize(2);
        assertThat(themesPassedToSaveAll.stream().map(Theme::getName)).containsExactlyInAnyOrder(THEME_NAME_2, THEME_NAME_3);


        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, TEST_ID_2), theme2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, THEME_NAME_2), theme2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, TEST_ID_3), theme3);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.THEME, THEME_NAME_3), theme3);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }

    @Test
    @DisplayName("createBulk_whenAllExist_shouldSkipAll")
    void createBulk_whenAllExist_shouldSkipAll() {

        List<String> namesToCreate = List.of(THEME_NAME_1, THEME_NAME_2);
        Set<String> uniqueLowerNames = Set.of(THEME_NAME_1.toLowerCase(), THEME_NAME_2.toLowerCase());
        Set<Theme> existingThemes = Set.of(theme1, theme2);

        when(themeRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingThemes);


        BulkCreationResult<String> result = themeService.createBulk(namesToCreate);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(THEME_NAME_1, THEME_NAME_2);

        verify(themeRepository).findByNamesIgnoreCase(uniqueLowerNames);
        verify(themeRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn")
    void createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn() {

        List<String> namesToCreate = Arrays.asList(null, "   ", "", null);


        BulkCreationResult<String> result = themeService.createBulk(namesToCreate);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(null, "   ", "", null);

        verify(themeRepository, never()).findByNamesIgnoreCase(any());
        verify(themeRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withNullInputList_shouldReturnEmptyResults")
    void createBulk_withNullInputList_shouldReturnEmptyResults() {

        BulkCreationResult<String> result = themeService.createBulk(null);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).isEmpty();
        verifyNoInteractions(themeRepository);
        verifyNoInteractions(cache);
    }




    @Test
    @DisplayName("convertToDto_shouldFetchRelatedBuildsAndConvert")
    void convertToDto_shouldFetchRelatedBuildsAndConvert() {

        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        when(buildRepository.findBuildIdAndNameByThemeId(theme1.getId())).thenReturn(relatedBuilds);


        ThemeDto dto = themeService.convertToDto(theme1);


        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(theme1.getId());
        assertThat(dto.name()).isEqualTo(theme1.getName());
        assertThat(dto.relatedBuilds()).hasSize(1);
        assertThat(dto.relatedBuilds().get(0).id()).isEqualTo(build1.getId());
        assertThat(dto.relatedBuilds().get(0).name()).isEqualTo(build1.getName());

        verify(buildRepository).findBuildIdAndNameByThemeId(theme1.getId());
    }

    @Test
    @DisplayName("convertToDto_withNullTheme_shouldReturnNull")
    void convertToDto_withNullTheme_shouldReturnNull() {

        ThemeDto dto = themeService.convertToDto(null);


        assertThat(dto).isNull();
        verifyNoInteractions(buildRepository);
    }

    @Test
    @DisplayName("instantiateEntity_shouldReturnNewThemeWithName")
    void instantiateEntity_shouldReturnNewThemeWithName() {

        String name = "NewTheme";


        Theme newTheme = themeService.instantiateEntity(name);


        assertThat(newTheme).isNotNull();
        assertThat(newTheme.getId()).isNull();
        assertThat(newTheme.getName()).isEqualTo(name);
    }
}