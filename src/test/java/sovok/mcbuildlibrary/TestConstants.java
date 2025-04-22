package sovok.mcbuildlibrary;

import java.util.List;
import java.util.Set;
import sovok.mcbuildlibrary.model.Author;
import sovok.mcbuildlibrary.model.Build;
import sovok.mcbuildlibrary.model.Color;
import sovok.mcbuildlibrary.model.Theme;

/**
 * Constants for use in unit and integration tests.
 */
public final class TestConstants {

    private TestConstants() {
    } // Prevent instantiation

    // --- Common IDs and Names ---
    public static final Long TEST_ID_1 = 1L;
    public static final Long TEST_ID_2 = 2L;
    public static final Long TEST_ID_3 = 3L;
    public static final String TEST_NAME_NEW = "NewTestName";
    public static final String TEST_NAME_NON_EXISTENT = "NonExistentName";
    public static final Long NON_EXISTENT_ID = 999L;

    // --- Author Specific ---
    public static final String AUTHOR_NAME_1 = "AuthorOne";
    public static final String AUTHOR_NAME_2 = "AuthorTwo";
    public static final String AUTHOR_NAME_3 = "AuthorThree";


    // --- Theme Specific ---
    public static final String THEME_NAME_1 = "ThemeOne";

    // --- Color Specific ---
    public static final String COLOR_NAME_1 = "ColorOne";

    // --- Build Specific ---
    public static final String BUILD_NAME_1 = "BuildOne";
    public static final String BUILD_NAME_2 = "BuildTwo";
    public static final String BUILD_DESC_1 = "Description for BuildOne";
    public static final byte[] TEST_SCHEM_BYTES = "schematic data".getBytes();
    public static final List<String> TEST_SCREENSHOTS = List.of("ss1.png", "ss2.png");

    // --- Cache Keys (Generated using InMemoryCache logic) ---
    public static final String AUTHOR_CACHE_KEY_ID_1 = "Author::1";
    public static final String AUTHOR_CACHE_KEY_NAME_1 = "Author::" + AUTHOR_NAME_1;
    public static final String THEME_CACHE_KEY_ID_1 = "Theme::1";
    public static final String THEME_CACHE_KEY_NAME_1 = "Theme::" + THEME_NAME_1;
    public static final String COLOR_CACHE_KEY_ID_1 = "Color::1";
    public static final String COLOR_CACHE_KEY_NAME_1 = "Color::" + COLOR_NAME_1;
    public static final String BUILD_CACHE_KEY_ID_1 = "Build::1";
    public static final String BUILD_CACHE_KEY_NAME_1 = "Build::" + BUILD_NAME_1;

    // --- DTOs ---
    // --- Helper Methods for Creating Test Objects ---
    public static Author createTestAuthor(Long id, String name) {
        return Author.builder().id(id).name(name).build();
    }

    public static Theme createTestTheme(Long id, String name) {
        return Theme.builder().id(id).name(name).build();
    }

    public static Color createTestColor(Long id, String name) {
        return Color.builder().id(id).name(name).build();
    }

    public static Build createTestBuild(Long id, String name, Set<Author> authors, Set<Theme> themes, Set<Color> colors) {
        return Build.builder()
                .id(id)
                .name(name)
                .authors(authors)
                .themes(themes)
                .colors(colors)
                .description(BUILD_DESC_1)
                .screenshots(TEST_SCREENSHOTS)
                .schemFile(TEST_SCHEM_BYTES)
                .build();
    }

}