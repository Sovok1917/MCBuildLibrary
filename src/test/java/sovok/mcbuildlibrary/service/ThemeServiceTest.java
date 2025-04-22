package sovok.mcbuildlibrary.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private Theme theme1;
    private Build build1; // Assume this build uses theme1

    @BeforeEach
    void setUp() {
        theme1 = createTestTheme(TEST_ID_1, THEME_NAME_1);
        Author authorStub = createTestAuthor(TEST_ID_1, AUTHOR_NAME_1);
        build1 = createTestBuild(TEST_ID_1, BUILD_NAME_1, Set.of(authorStub), Set.of(theme1), Set.of());
    }

    // --- Test Cases for BaseNamedEntityService Logic (Subset) ---

    @Test
    @DisplayName("create_whenNameDoesNotExist_shouldSaveAndCacheTheme")
    void create_whenNameDoesNotExist_shouldSaveAndCacheTheme() {
        // Arrange
        when(themeRepository.findByName(THEME_NAME_1)).thenReturn(Optional.empty());
        when(themeRepository.save(any(Theme.class))).thenReturn(theme1);

        // Act
        Theme createdTheme = themeService.create(THEME_NAME_1);

        // Assert
        assertThat(createdTheme).isNotNull();
        assertThat(createdTheme.getName()).isEqualTo(THEME_NAME_1);
        verify(themeRepository).save(any(Theme.class));
        verify(cache).put(THEME_CACHE_KEY_ID_1, createdTheme);
        verify(cache).put(THEME_CACHE_KEY_NAME_1, createdTheme);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
    }


    // --- Test Cases Specific to ThemeService ---

    @Test
    @DisplayName("deleteById_whenThemeIsAssociatedWithBuilds_shouldThrowIllegalStateException")
    void deleteById_whenThemeIsAssociatedWithBuilds_shouldThrowIllegalStateException() {
        // Arrange
        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(TEST_ID_1)).thenReturn(List.of(build1)); // Associated build
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached

        // Act & Assert
        assertThatThrownBy(() -> themeService.deleteById(TEST_ID_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(String.format(StringConstants.CANNOT_DELETE_ASSOCIATED,
                        StringConstants.THEME, theme1.getName(), 1)); // Check message format

        verify(themeRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByThemeId(TEST_ID_1); // checkDeletionConstraints called
        verify(themeRepository, never()).delete(any()); // Delete should not be called
        verify(cache, never()).evict(anyString()); // Cache should not be evicted
    }

    @Test
    @DisplayName("deleteById_whenThemeIsNotAssociated_shouldDeleteAndEvictCaches")
    void deleteById_whenThemeIsNotAssociated_shouldDeleteAndEvictCaches() {
        // Arrange
        when(themeRepository.findById(TEST_ID_1)).thenReturn(Optional.of(theme1));
        when(buildRepository.findBuildsByThemeId(TEST_ID_1)).thenReturn(Collections.emptyList()); // NO associated builds
        when(cache.get(THEME_CACHE_KEY_ID_1)).thenReturn(Optional.empty()); // Assume not cached

        // Act
        themeService.deleteById(TEST_ID_1);

        // Assert
        verify(themeRepository).findById(TEST_ID_1);
        verify(buildRepository).findBuildsByThemeId(TEST_ID_1); // checkDeletionConstraints called
        verify(themeRepository).delete(theme1); // Delete SHOULD be called

        // Verify cache evictions
        verify(cache).evict(THEME_CACHE_KEY_ID_1);
        verify(cache).evict(THEME_CACHE_KEY_NAME_1);
        verify(cache).evictQueryCacheByType(StringConstants.THEME);
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
        when(buildRepository.findBuildIdAndNameByThemeId(theme1.getId())).thenReturn(relatedBuilds);

        // Act
        ThemeDto dto = themeService.convertToDto(theme1);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(theme1.getId());
        assertThat(dto.name()).isEqualTo(theme1.getName());
        assertThat(dto.relatedBuilds()).hasSize(1);
        assertThat(dto.relatedBuilds().get(0).id()).isEqualTo(build1.getId());


        verify(buildRepository).findBuildIdAndNameByThemeId(theme1.getId());
    }

    @Test
    @DisplayName("convertToDto_withNullTheme_shouldReturnNull") // New Test for Branch
    void convertToDto_withNullTheme_shouldReturnNull() {
        // Act
        ThemeDto dto = themeService.convertToDto(null);

        // Assert
        assertThat(dto).isNull();
        // Verify no interaction with build repository when input is null
        verifyNoInteractions(buildRepository);
    }

    @Test
    @DisplayName("instantiateEntity_shouldReturnNewThemeWithName")
    void instantiateEntity_shouldReturnNewThemeWithName() {
        // Arrange
        String name = "NewTheme";

        // Act
        Theme newTheme = themeService.instantiateEntity(name);

        // Assert
        assertThat(newTheme).isNotNull();
        assertThat(newTheme.getId()).isNull();
        assertThat(newTheme.getName()).isEqualTo(name);
    }
}