// file: src/test/java/sovok/mcbuildlibrary/service/ColorServiceTest.java
package sovok.mcbuildlibrary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sovok.mcbuildlibrary.cache.InMemoryCache;
import sovok.mcbuildlibrary.dto.ColorDto;
import sovok.mcbuildlibrary.dto.RelatedBuildDto;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.repository.BuildRepository;
import sovok.mcbuildlibrary.repository.ColorRepository;

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

    private Color color1;
    private Color color2;
    private Build build1; // Assume this build uses color1

    @BeforeEach
    void setUp() {
        color1 = createTestColor(TEST_ID_1, COLOR_NAME_1);
        color2 = createTestColor(TEST_ID_2, COLOR_NAME_2);
        Author authorStub = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);
        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(authorStub), Set.of(), Set.of(color1));
    }

    // --- Test Cases for BaseNamedEntityService Logic (Subset - showing key ones) ---

    @Test
    @DisplayName("create_whenNameDoesNotExist_shouldSaveAndCacheColor")
    void create_whenNameDoesNotExist_shouldSaveAndCacheColor() {
        // Arrange
        when(colorRepository.findByName(COLOR_NAME_1)).thenReturn(Optional.empty());
        when(colorRepository.save(any(Color.class))).thenReturn(color1);

        // Act
        Color createdColor = colorService.create(COLOR_NAME_1);

        // Assert
        assertThat(createdColor).isNotNull();
        assertThat(createdColor.getName()).isEqualTo(COLOR_NAME_1);
        verify(colorRepository).save(any(Color.class));
        verify(cache).put(eq(COLOR_CACHE_KEY_ID_1), eq(createdColor));
        verify(cache).put(eq(COLOR_CACHE_KEY_NAME_1), eq(createdColor));
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
    }

    @Test
    @DisplayName("findById_whenCached_shouldReturnCachedColor")
    void findById_whenCached_shouldReturnCachedColor() {
        // Arrange
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.of(color1));

        // Act
        Optional<Color> foundColor = colorService.findById(TEST_ID_1);

        // Assert
        assertThat(foundColor).isPresent().contains(color1);
        verify(colorRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("findDtoById_whenExists_shouldFetchAndConvertToDto")
    void findDtoById_whenExists_shouldFetchAndConvertToDto() {
        // Arrange
        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached
        when(buildRepository.findBuildIdAndNameByColorId(TEST_ID_1)).thenReturn(relatedBuilds);

        // Act
        Optional<ColorDto> foundDto = colorService.findDtoById(TEST_ID_1);

        // Assert
        assertThat(foundDto).isPresent();
        assertThat(foundDto.get().id()).isEqualTo(TEST_ID_1);
        assertThat(foundDto.get().name()).isEqualTo(COLOR_NAME_1);
        assertThat(foundDto.get().relatedBuilds()).hasSize(1);
        verify(buildRepository).findBuildIdAndNameByColorId(TEST_ID_1);
        verify(cache).put(COLOR_CACHE_KEY_ID_1, color1); // Verify caching
    }

    // --- Test Cases Specific to ColorService ---

    @Test
    @DisplayName("deleteById_whenColorIsAssociatedWithBuilds_shouldThrowIllegalStateException")
    void deleteById_whenColorIsAssociatedWithBuilds_shouldThrowIllegalStateException() {
        // Arrange
        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(TEST_ID_1)).thenReturn(List.of(build1)); // Associated build
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached

        // Act & Assert
        assertThatThrownBy(() -> colorService.deleteById(TEST_ID_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.COLOR, color1.getName(), 1)); // Check message format

        verify(colorRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByColorId(TEST_ID_1); // checkDeletionConstraints called
        verify(colorRepository, never()).delete(any()); // Delete should not be called
        verify(cache, never()).evict(anyString()); // Cache should not be evicted
    }

    @Test
    @DisplayName("deleteById_whenColorIsNotAssociated_shouldDeleteAndEvictCaches")
    void deleteById_whenColorIsNotAssociated_shouldDeleteAndEvictCaches() {
        // Arrange
        when(colorRepository.findById(TEST_ID_1)).thenReturn(Optional.of(color1));
        when(buildRepository.findBuildsByColorId(TEST_ID_1)).thenReturn(Collections.emptyList()); // NO associated builds
        when(cache.get(COLOR_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached

        // Act
        colorService.deleteById(TEST_ID_1);

        // Assert
        verify(colorRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByColorId(TEST_ID_1); // checkDeletionConstraints called
        verify(colorRepository).delete(color1); // Delete SHOULD be called

        // Verify cache evictions
        verify(cache).evict(COLOR_CACHE_KEY_ID_1);
        verify(cache).evict(COLOR_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.COLOR);
        // Build cache SHOULD NOT be evicted here
        verify(cache, never()).evictQueryCacheByType(StringConstants.BUILD);
    }

    @Test
    @DisplayName("convertToDto_shouldFetchRelatedBuildsAndConvert")
    void convertToDto_shouldFetchRelatedBuildsAndConvert() {
        // Arrange
        List<BuildRepository.BuildIdAndName> relatedBuilds = List.of(
                new BuildRepository.BuildIdAndName() {
                    public Long getId() { return build1.getId(); }
                    public String getName() { return build1.getName(); }
                }
        );
        when(buildRepository.findBuildIdAndNameByColorId(color1.getId())).thenReturn(relatedBuilds);

        // Act
        ColorDto dto = colorService.convertToDto(color1);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(color1.getId());
        assertThat(dto.name()).isEqualTo(color1.getName());
        assertThat(dto.relatedBuilds()).hasSize(1);
        assertThat(dto.relatedBuilds().get(0).id()).isEqualTo(build1.getId());

        verify(buildRepository).findBuildIdAndNameByColorId(color1.getId());
    }

    @Test
    @DisplayName("convertToDto_withNullColor_shouldReturnNull") // New Test for Branch
    void convertToDto_withNullColor_shouldReturnNull() {
        // Act
        ColorDto dto = colorService.convertToDto(null);

        // Assert
        assertThat(dto).isNull();
        // Verify no interaction with build repository when input is null
        verifyNoInteractions(buildRepository);
    }

    @Test
    @DisplayName("instantiateEntity_shouldReturnNewColorWithName")
    void instantiateEntity_shouldReturnNewColorWithName() {
        // Arrange
        String name = "NewColor";

        // Act
        Color newColor = colorService.instantiateEntity(name);

        // Assert
        assertThat(newColor).isNotNull();
        assertThat(newColor.getId()).isNull();
        assertThat(newColor.getName()).isEqualTo(name);
    }
}