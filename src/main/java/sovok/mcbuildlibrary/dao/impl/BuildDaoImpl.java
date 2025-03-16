package sovok.mcbuildlibrary.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.dao.BuildDao;
import sovok.mcbuildlibrary.model.Build;

@Repository
public class BuildDaoImpl implements BuildDao {

    // Constants for reused strings.
    private static final String AUTHOR1 = "Author1";
    private static final String MEDIEVAL = "Medieval";
    private static final String FANTASY = "Fantasy";

    // Simulated database of builds using an in-memory List.
    private final List<Build> builds = new ArrayList<>(List.of(
            Build.builder()
                    .id("1")
                    .name("Medieval Castle")
                    .author(AUTHOR1)
                    .theme(MEDIEVAL)
                    .description("A majestic medieval castle with intricate details.")
                    .colors(List.of("Gray", "Blue"))
                    .screenshots(List.of("https://example.com/medieval1.jpg", "https://example.com/medieval2.jpg"))
                    .schemFilePath("https://example.com/medieval.schem")
                    .build(),
            Build.builder()
                    .id("2")
                    .name("Modern Villa")
                    .author(AUTHOR1)
                    .theme(FANTASY)
                    .description("A sleek modern villa with a swimming pool.")
                    .colors(List.of("White", "Black", "Gray"))
                    .screenshots(List.of("https://example.com/modern1.jpg", "https://example.com/modern2.jpg"))
                    .schemFilePath("https://example.com/modern.schem")
                    .build()
    ));

    @Override
    public Optional<Build> findById(String id) {
        return builds.stream()
                .filter(build -> build.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Build> findAll() {
        return builds.stream()
                .toList();
    }

    @Override
    public Optional<Build> findByName(String name) {
        return builds.stream()
                .filter(build -> build.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Build> findByTheme(String theme) {
        return builds.stream()
                .filter(build -> build.getTheme().equalsIgnoreCase(theme))
                .toList();
    }

    @Override
    public List<Build> findByAuthor(String author) {
        return builds.stream()
                .filter(build -> build.getAuthor().equalsIgnoreCase(author))
                .toList();
    }

    @Override
    public List<Build> filterBuilds(String author, String name, String theme, List<String> colors) {
        return builds.stream()
                .filter(build -> author == null || build.getAuthor().equalsIgnoreCase(author))
                .filter(build -> name == null || build.getName().equalsIgnoreCase(name))
                .filter(build -> theme == null || build.getTheme().equalsIgnoreCase(theme))
                .filter(build -> {
                    if (colors == null || colors.isEmpty()) {
                        return true;
                    }
                    return build.getColors()
                            .stream()
                            .anyMatch(buildColor ->
                                    colors.stream().anyMatch(buildColor::equalsIgnoreCase)
                            );
                })
                .toList();
    }
}
