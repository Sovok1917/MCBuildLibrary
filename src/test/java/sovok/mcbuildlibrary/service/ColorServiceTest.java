
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
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;
import sovok.mcbuildlibrary.util.BulkCreationResult;


import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static sovok.mcbuildlibrary.TestConstants.*;

@ExtendWith(MockitoExtension.class)
class ColorServiceTest {

    @Mock
    private ColorRepository colorRepository;
    @Mock
    private BuildRepository buildRepository;
    @Mock
    private InMemoryCache cache;

    @InjectMocks
    private ColorService colorService;

    @Captor
    private ArgumentCaptor<Color> colorCaptor;
    @Captor
    private ArgumentCaptor<List<Color>> colorListCaptor;


    private Color color1;
    private Color color2;
    private Color color3;
    private Build build1;

    @BeforeEach
    void setUp() {
        color1 = createTestColor(TEST_ID_1, COLOR_NAME_1);
        color2 = createTestColor(TEST_ID_2, COLOR_NAME_2);
        color3 = createTestColor(TEST_ID_3, COLOR_NAME_3);
        Author authorStub = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);

        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(authorStub), Set.of(), Set.of(color1));
    }



    @Test
    @DisplayName("create_whenNameDoesNotExist_shouldSaveAndCacheColor")
    void create_whenNameDoesNotExist_shouldSaveAndCacheColor() {

        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.empty());
        when(colorRepository.save(any(Color.class))).thenReturn(color1);


        Color createdColor = colorService.create(COLOR_NAME_1);


        assertThat(createdColor).isNotNull();
        assertThat(createdColor.getName()).isEqualTo(COLOR_NAME_1);
        assertThat(createdColor.getId()).isEqualTo(TEST_ID_1);

        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(colorRepository).save(colorCaptor.capture());
        assertThat(colorCaptor.getValue().getName()).isEqualTo(COLOR_NAME_1);
        assertThat(colorCaptor.getValue().getId()).isNull();

        verify(cache).put(COLOR_CACHE_KEY_ID_1, createdColor);
        verify(cache).put(COLOR_CACHE_KEY_NAME_1, createdColor);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("create_whenNameExists_shouldThrowIllegalArgumentException")
    void create_whenNameExists_shouldThrowIllegalArgumentException() {

        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));


        assertThatThrownBy(() -> colorService.create(COLOR_NAME_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.format(StringConstants.RESOURCE_ALREADY_EXISTS_TEMPLATE,
                        StringConstants.COLOR, StringConstants.WITH_NAME, COLOR_NAME_1, StringConstants.ALREADY_EXISTS_MESSAGE));

        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(colorRepository, never()).save(any());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }


    @Test
    @DisplayName("findById_whenCached_shouldReturnCachedColor")
    void findById_whenCached_shouldReturnCachedColor() {

        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.of(color1));


        Optional<Color> foundColor = colorService.findById(TEST_ID_1);


        assertThat(foundColor).isPresent().contains(color1);
        verify(cache).get(COLOR_CACHE_KEY_ID_1);
        verify(colorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("findById_whenNotCachedButExists_shouldFetchAndCache")
    void findById_whenNotCachedButExists_shouldFetchAndCache() {

        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));


        Optional<Color> foundColor = colorService.findById(TEST_ID_1);


        assertThat(foundColor).isPresent().contains(color1);
        verify(cache).get(COLOR_CACHE_KEY_ID_1);
        verify(colorRepository).findById(TEST_ID_1);
        verify(cache).put(COLOR_CACHE_KEY_ID_1, color1);
    }

    @Test
    @DisplayName("findById_whenNotFound_shouldReturnEmpty")
    void findById_whenNotFound_shouldReturnEmpty() {

        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());
        when(colorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());


        Optional<Color> foundColor = colorService.findById(NON_EXISTENT_ID);


        assertThat(foundColor).isEmpty();
        verify(cache).get(InMemoryCache.generateKey(StringConstants.COLOR, NON_EXISTENT_ID));
        verify(colorRepository).findById(NON_EXISTENT_ID);
        verify(cache, never()).put(anyString(), any());
    }

    @Test
    @DisplayName("findByName_whenCachedAndMatches_shouldReturnCached")
    void findByName_whenCachedAndMatches_shouldReturnCached() {

        when(cache.get(COLOR_CACHE_KEY_NAME_1)).thenReturn(Optional.of(color1));


        Optional<Color> foundColor = colorService.findByName(COLOR_NAME_1);


        assertThat(foundColor).isPresent().contains(color1);
        verify(cache).get(COLOR_CACHE_KEY_NAME_1);
        verify(cache, never()).evict(anyString());
        verify(colorRepository, never()).findByName(anyString());
    }

    @Test
    @DisplayName("findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo")
    void findByName_whenCachedButNameMismatch_shouldEvictAndFetchFromRepo() {

        String requestedNameLower = COLOR_NAME_1.toLowerCase();
        String cacheKey = InMemoryCache.generateKey(StringConstants.COLOR, requestedNameLower);
        when(cache.get(cacheKey)).thenReturn(Optional.of(color1));
        when(colorRepository.findByName(requestedNameLower)).thenReturn(Optional.of(color1));


        Optional<Color> foundColor = colorService.findByName(requestedNameLower);


        assertThat(foundColor).isPresent().contains(color1);
        verify(cache).get(cacheKey);
        verify(cache).evict(cacheKey);
        verify(colorRepository).findByName(requestedNameLower);
        verify(cache).put(cacheKey, color1);
    }

    @Test
    @DisplayName("findByName_whenNotCachedButExists_shouldFetchAndCache")
    void findByName_whenNotCachedButExists_shouldFetchAndCache() {

        when(cache.get(COLOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());
        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));


        Optional<Color> foundColor = colorService.findByName(COLOR_NAME_1);


        assertThat(foundColor).isPresent().contains(color1);
        verify(cache).get(COLOR_CACHE_KEY_NAME_1);
        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(cache).put(COLOR_CACHE_KEY_NAME_1, color1);
    }

    @Test
    @DisplayName("findByName_whenNotFound_shouldReturnEmpty")
    void findByName_whenNotFound_shouldReturnEmpty() {

        String nonExistentName = TEST_NAME_NON_EXISTENT;
        String cacheKey = InMemoryCache.generateKey(StringConstants.COLOR, nonExistentName);
        when(cache.get(cacheKey)).thenReturn(Optional.empty());
        when(colorRepository.findByName(nonExistentName)).thenReturn(Optional.empty());


        Optional<Color> foundColor = colorService.findByName(nonExistentName);


        assertThat(foundColor).isEmpty();
        verify(cache).get(cacheKey);
        verify(colorRepository).findByName(nonExistentName);
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
        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds);


        Optional<ColorDto> foundDto = colorService.findDtoById(TEST_ID_1);


        assertThat(foundDto).isPresent();
        assertThat(foundDto.get().id()).isEqualTo(TEST_ID_1);
        assertThat(foundDto.get().name()).isEqualTo(COLOR_NAME_1);
        assertThat(foundDto.get().relatedBuilds()).hasSize(1);
        assertThat(foundDto.get().relatedBuilds().get(0).id()).isEqualTo(build1.getId());
        assertThat(foundDto.get().relatedBuilds().get(0).name()).isEqualTo(build1.getName());

        verify(colorRepository).findById(TEST_ID_1);
        verify(cache).put(COLOR_CACHE_KEY_ID_1, color1);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtoById_whenNotFound_shouldReturnEmpty")
    void findDtoById_whenNotFound_shouldReturnEmpty() {

        when(colorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        Optional<ColorDto> foundDto = colorService.findDtoById(NON_EXISTENT_ID);


        assertThat(foundDto).isEmpty();
        verify(colorRepository).findById(NON_EXISTENT_ID);
        verify(buildRepository, never()).findBuildIdAndNameByColorId(anyLong());
    }


    @Test
    @DisplayName("findAllDtos_shouldFetchAllFromRepoAndConvert")
    void findAllDtos_shouldFetchAllFromRepoAndConvert() {

        List<Color> colors = List.of(color1, color2);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = Collections.emptyList();

        when(colorRepository.findAll()).thenReturn(colors);
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_2)).thenReturn(relatedBuilds2);


        List<ColorDto> allDtos = colorService.findAllDtos();


        assertThat(allDtos).hasSize(2);
        assertThat(allDtos.get(0).id()).isEqualTo(TEST_ID_1);
        assertThat(allDtos.get(0).name()).isEqualTo(COLOR_NAME_1);
        assertThat(allDtos.get(0).relatedBuilds()).hasSize(1);
        assertThat(allDtos.get(1).id()).isEqualTo(TEST_ID_2);
        assertThat(allDtos.get(1).name()).isEqualTo(COLOR_NAME_2);
        assertThat(allDtos.get(1).relatedBuilds()).isEmpty();

        verify(colorRepository).findAll();
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_2);
    }

    @Test
    @DisplayName("findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert")
    void findDtosByNameQuery_whenNotCached_shouldQueryRepoCacheAndConvert() {

        String query = "Color";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.COLOR, params);
        List<Color> repoResult = List.of(color1);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(colorRepository.fuzzyFindByName(query)).thenReturn(repoResult);
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds1);


        List<ColorDto> queryDtos = colorService.findDtosByNameQuery(query);


        assertThat(queryDtos).hasSize(1);
        assertThat(queryDtos.get(0).name()).isEqualTo(COLOR_NAME_1);

        verify(cache).get(queryKey);
        verify(colorRepository).fuzzyFindByName(query);
        verify(cache).put(queryKey, repoResult);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert")
    void findDtosByNameQuery_whenCached_shouldReturnCachedAndConvert() {

        String query = "Color";
        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, query);
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.COLOR, params);
        List<Color> cachedRepoResult = List.of(color1);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.of(cachedRepoResult));
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds1);


        List<ColorDto> queryDtos = colorService.findDtosByNameQuery(query);


        assertThat(queryDtos).hasSize(1);
        assertThat(queryDtos.get(0).name()).isEqualTo(COLOR_NAME_1);

        verify(cache).get(queryKey);
        verify(colorRepository, never()).fuzzyFindByName(any());
        verify(cache, never()).put(anyString(), any());
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
    }

    @Test
    @DisplayName("findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery")
    void findDtosByNameQuery_withNullName_shouldHandleNullInKeyAndQuery() {

        Map<String, Object> params = Map.of(StringConstants.NAME_REQ_PARAM, "__NULL__");
        String queryKey = InMemoryCache.generateQueryKey(StringConstants.COLOR, params);
        List<Color> repoResult = List.of(color1, color2);
        List<BuildRepository.BuildIdAndName> relatedBuilds1 = List.of();
        List<BuildRepository.BuildIdAndName> relatedBuilds2 = List.of();

        when(cache.get(queryKey)).thenReturn(Optional.empty());
        when(colorRepository.fuzzyFindByName(null)).thenReturn(repoResult);
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds1);
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_2)).thenReturn(relatedBuilds2);


        List<ColorDto> queryDtos = colorService.findDtosByNameQuery(null);


        assertThat(queryDtos).hasSize(2);
        verify(cache).get(queryKey);
        verify(colorRepository).fuzzyFindByName(null);
        verify(cache).put(queryKey, repoResult);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_2);
    }

    @Test
    @DisplayName("update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries")
    void update_whenExistsAndNameUnique_shouldUpdateCacheAndEvictQueries() {

        Color updatedColor = createTestColor(TEST_ID_1, TEST_NAME_NEW);
        String newNameCacheKey = InMemoryCache.generateKey(StringConstants.COLOR, TEST_NAME_NEW);

        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(colorRepository.findByName(TEST_NAME_NEW)).thenReturn(Optional.empty());
        when(colorRepository.save(any(Color.class))).thenReturn(updatedColor);
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        Color result = colorService.update(TEST_ID_1, TEST_NAME_NEW);


        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(TEST_ID_1);
        assertThat(result.getName()).isEqualTo(TEST_NAME_NEW);

        verify(colorRepository).findById(TEST_ID_1);
        verify(colorRepository).findByName(TEST_NAME_NEW);
        verify(colorRepository).save(colorCaptor.capture());
        assertThat(colorCaptor.getValue().getName()).isEqualTo(TEST_NAME_NEW);

        verify(cache).evict(COLOR_CACHE_KEY_NAME_1);
        verify(cache).put(newNameCacheKey, updatedColor);
        verify(cache, atLeastOnce()).put(eq(COLOR_CACHE_KEY_ID_1), any(Color.class));
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName")
    void update_whenNameDoesNotChange_shouldUpdateCacheButNotEvictOldName() {

        Color savedColor = createTestColor(TEST_ID_1, COLOR_NAME_1);

        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));
        when(colorRepository.save(any(Color.class))).thenReturn(savedColor);
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        Color result = colorService.update(TEST_ID_1, COLOR_NAME_1);


        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(COLOR_NAME_1);

        verify(colorRepository).findById(TEST_ID_1);
        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(colorRepository).save(any(Color.class));

        verify(cache, never()).evict(COLOR_CACHE_KEY_NAME_1);
        verify(cache).put(COLOR_CACHE_KEY_NAME_1, savedColor);
        verify(cache, atLeastOnce()).put(eq(COLOR_CACHE_KEY_ID_1), any(Color.class));
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException")
    void update_whenNameExistsForDifferentId_shouldThrowIllegalArgumentException() {

        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(colorRepository.findByName(COLOR_NAME_2)).thenReturn(Optional.of(color2));
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.update(TEST_ID_1, COLOR_NAME_2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(COLOR_NAME_2 + "' " + StringConstants.ALREADY_EXISTS_MESSAGE);

        verify(colorRepository).findById(TEST_ID_1);
        verify(colorRepository).findByName(COLOR_NAME_2);
        verify(colorRepository, never()).save(any());
        verify(cache, never()).evict(anyString());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("update_whenNotFound_shouldThrowNoSuchElementException")
    void update_whenNotFound_shouldThrowNoSuchElementException() {

        when(colorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.update(NON_EXISTENT_ID, TEST_NAME_NEW))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.COLOR + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(colorRepository).findById(NON_EXISTENT_ID);
        verify(colorRepository, never()).findByName(anyString());
        verify(colorRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById_whenColorIsAssociatedWithBuilds_shouldThrowIllegalStateException")
    void deleteById_whenColorIsAssociatedWithBuilds_shouldThrowIllegalStateException() {

        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(TEST_ID_1)).thenReturn(List.of(build1));
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.deleteById(TEST_ID_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.COLOR, color1.getName(), 1));

        verify(colorRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByColorId(TEST_ID_1);
        verify(colorRepository, never()).delete(any());
        verify(cache, never()).evict(anyString());
    }

    @Test
    @DisplayName("deleteById_whenColorIsNotAssociated_shouldDeleteAndEvictCaches")
    void deleteById_whenColorIsNotAssociated_shouldDeleteAndEvictCaches() {

        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(TEST_ID_1)).thenReturn(Collections.emptyList());
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty());


        colorService.deleteById(TEST_ID_1);


        verify(colorRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByColorId(TEST_ID_1);
        verify(colorRepository).delete(color1);


        verify(cache).evict(COLOR_CACHE_KEY_ID_1);
        verify(cache).evict(COLOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
        verify(cache, never()).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("deleteById_whenNotFound_shouldThrowNoSuchElementException")
    void deleteById_whenNotFound_shouldThrowNoSuchElementException() {

        when(colorRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, NON_EXISTENT_ID))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.deleteById(NON_EXISTENT_ID))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.COLOR + " " + StringConstants.WITH_ID + " '" + NON_EXISTENT_ID + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(colorRepository).findById(NON_EXISTENT_ID);
        verify(colorRepository, never()).delete(any());
        verify(buildRepository, never()).findBuildsByColorId(anyLong());
        verify(cache, never()).evict(anyString());
    }

    @Test
    @DisplayName("deleteByName_whenExistsAndNotAssociated_shouldDeleteAndEvict")
    void deleteByName_whenExistsAndNotAssociated_shouldDeleteAndEvict() {

        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(color1.getId())).thenReturn(Collections.emptyList());
        when(cache.get(COLOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        colorService.deleteByName(COLOR_NAME_1);


        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(buildRepository).findBuildsByColorId(color1.getId());
        verify(colorRepository).delete(color1);
        verify(cache).evict(COLOR_CACHE_KEY_ID_1);
        verify(cache).evict(COLOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("deleteByName_whenExistsAndAssociated_shouldThrowIllegalStateException")
    void deleteByName_whenExistsAndAssociated_shouldThrowIllegalStateException() {

        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(color1.getId())).thenReturn(List.of(build1));
        when(cache.get(COLOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.deleteByName(COLOR_NAME_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.COLOR, color1.getName(), 1));

        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(buildRepository).findBuildsByColorId(color1.getId());
        verify(colorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteByName_whenNotFound_shouldThrowNoSuchElementException")
    void deleteByName_whenNotFound_shouldThrowNoSuchElementException() {

        when(colorRepository.findByName(TEST_NAME_NON_EXISTENT)).thenReturn(Optional.empty());
        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, TEST_NAME_NON_EXISTENT))).thenReturn(Optional.empty());


        assertThatThrownBy(() -> colorService.deleteByName(TEST_NAME_NON_EXISTENT))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(StringConstants.COLOR + " " + StringConstants.WITH_NAME + " '" + TEST_NAME_NON_EXISTENT + "' " + StringConstants.NOT_FOUND_MESSAGE);

        verify(colorRepository).findByName(TEST_NAME_NON_EXISTENT);
        verify(colorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("findOrCreate_whenExists_shouldReturnExisting")
    void findOrCreate_whenExists_shouldReturnExisting() {

        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.of(color1));
        when(cache.get(COLOR_CACHE_KEY_NAME_1)).thenReturn(Optional.empty());


        Color result = colorService.findOrCreate(COLOR_NAME_1);


        assertThat(result).isEqualTo(color1);
        verify(colorRepository).findByName(COLOR_NAME_1);
        verify(colorRepository, never()).save(any());
        verify(cache).put(COLOR_CACHE_KEY_NAME_1, color1);
    }

    @Test
    @DisplayName("findOrCreate_whenNotExists_shouldCreateAndReturnNew")
    void findOrCreate_whenNotExists_shouldCreateAndReturnNew() {

        String newName = "NewColorToCreate";
        Color newlyCreatedColor = createTestColor(TEST_ID_3, newName);

        when(colorRepository.findByName(newName)).thenReturn(Optional.empty());
        when(colorRepository.save(any(Color.class))).thenReturn(newlyCreatedColor);
        when(cache.get(InMemoryCache.generateKey(StringConstants.COLOR, newName))).thenReturn(Optional.empty());


        Color result = colorService.findOrCreate(newName);


        assertThat(result).isEqualTo(newlyCreatedColor);

        verify(colorRepository, times(2)).findByName(newName);
        verify(colorRepository).save(colorCaptor.capture());
        assertThat(colorCaptor.getValue().getName()).isEqualTo(newName);

        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, newlyCreatedColor.getId()), newlyCreatedColor);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, newName), newlyCreatedColor);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("createBulk_whenSomeExist_shouldCreateNewAndSkipExisting")
    void createBulk_whenSomeExist_shouldCreateNewAndSkipExisting() {

        List<String> namesToCreate = List.of(COLOR_NAME_1, COLOR_NAME_2, COLOR_NAME_3, "  " + COLOR_NAME_1 + "  ");
        Set<String> uniqueLowerNames = Set.of(COLOR_NAME_1.toLowerCase(), COLOR_NAME_2.toLowerCase(), COLOR_NAME_3.toLowerCase());
        Set<Color> existingColors = Set.of(color1);
        List<Color> savedEntities = List.of(color2, color3);

        when(colorRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingColors);
        when(colorRepository.saveAll(anyList())).thenReturn(savedEntities);


        BulkCreationResult<String> result = colorService.createBulk(namesToCreate);


        assertThat(result.createdItems()).containsExactlyInAnyOrder(COLOR_NAME_2, COLOR_NAME_3);
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(COLOR_NAME_1);

        verify(colorRepository).findByNamesIgnoreCase(uniqueLowerNames);
        verify(colorRepository).saveAll(colorListCaptor.capture());
        List<Color> colorsPassedToSaveAll = colorListCaptor.getValue();
        assertThat(colorsPassedToSaveAll).hasSize(2);
        assertThat(colorsPassedToSaveAll.stream().map(Color::getName)).containsExactlyInAnyOrder(COLOR_NAME_2, COLOR_NAME_3);


        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, TEST_ID_2), color2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, COLOR_NAME_2), color2);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, TEST_ID_3), color3);
        verify(cache).put(InMemoryCache.generateKey(StringConstants.COLOR, COLOR_NAME_3), color3);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("createBulk_whenAllExist_shouldSkipAll")
    void createBulk_whenAllExist_shouldSkipAll() {

        List<String> namesToCreate = List.of(COLOR_NAME_1, COLOR_NAME_2);
        Set<String> uniqueLowerNames = Set.of(COLOR_NAME_1.toLowerCase(), COLOR_NAME_2.toLowerCase());
        Set<Color> existingColors = Set.of(color1, color2);

        when(colorRepository.findByNamesIgnoreCase(uniqueLowerNames)).thenReturn(existingColors);


        BulkCreationResult<String> result = colorService.createBulk(namesToCreate);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(COLOR_NAME_1, COLOR_NAME_2);

        verify(colorRepository).findByNamesIgnoreCase(uniqueLowerNames);
        verify(colorRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn")
    void createBulk_withOnlyInvalidNames_shouldSkipAllAndReturn() {

        List<String> namesToCreate = Arrays.asList(null, "   ", "", null);


        BulkCreationResult<String> result = colorService.createBulk(namesToCreate);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).containsExactlyInAnyOrder(null, "   ", "", null);

        verify(colorRepository, never()).findByNamesIgnoreCase(any());
        verify(colorRepository, never()).saveAll(anyList());
        verify(cache, never()).put(anyString(), any());
        verify(cache, never()).evictQueryCacheByType(anyString());
    }

    @Test
    @DisplayName("createBulk_withNullInputList_shouldReturnEmptyResults")
    void createBulk_withNullInputList_shouldReturnEmptyResults() {

        BulkCreationResult<String> result = colorService.createBulk(null);


        assertThat(result.createdItems()).isEmpty();
        assertThat(result.skippedItems()).isEmpty();
        verifyNoInteractions(colorRepository);
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
        when(buildRepository.findBuildIdAndNameByColorId(color1.getId())).thenReturn(relatedBuilds);


        ColorDto dto = colorService.convertToDto(color1);


        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(color1.getId());
        assertThat(dto.name()).isEqualTo(color1.getName());
        assertThat(dto.relatedBuilds()).hasSize(1);
        assertThat(dto.relatedBuilds().get(0).id()).isEqualTo(build1.getId());
        assertThat(dto.relatedBuilds().get(0).name()).isEqualTo(build1.getName());

        verify(buildRepository).findBuildIdAndNameByColorId(color1.getId());
    }

    @Test
    @DisplayName("convertToDto_withNullColor_shouldReturnNull")
    void convertToDto_withNullColor_shouldReturnNull() {

        ColorDto dto = colorService.convertToDto(null);


        assertThat(dto).isNull();
        verifyNoInteractions(buildRepository);
    }

    @Test
    @DisplayName("instantiateEntity_shouldReturnNewColorWithName")
    void instantiateEntity_shouldReturnNewColorWithName() {

        String name = "NewColor";


        Color newColor = colorService.instantiateEntity(name);


        assertThat(newColor).isNotNull();
        assertThat(newColor.getId()).isNull();
        assertThat(newColor.getName()).isEqualTo(name);
    }
}