package sovok.mcbuildlibrary.dao.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.dao.BuildDao;
import sovok.mcbuildlibrary.model.Build;

@Repository
public class BuildDaoImpl implements BuildDao {

    // Simulated database of builds using an in-memory List.
    private final List<Build> builds = new ArrayList<>(List.of(
            new Build("1", "Medieval Castle", "Author1", "Medieval",
                    "A majestic medieval castle with intricate details.",
                    new ArrayList<>(List.of("Gray", "Blue")),
                    new ArrayList<>(List.of("https://example.com/medieval1.jpg", "https://example.com/medieval2.jpg")),
                    "https://example.com/medieval.schem"),
            new Build("2", "Modern Villa", "Author1", "Fantasy",
                    "A sleek modern villa with a swimming pool.",
                    new ArrayList<>(List.of("White", "Black", "Gray")),
                    new ArrayList<>(List.of("https://example.com/modern1.jpg", "https://example.com/modern2.jpg")),
                    "https://example.com/modern.schem"),
            new Build("3", "Fantasy Treehouse", "Author2", "Fantasy",
                    "An enchanting treehouse in a fantasy setting.",
                    new ArrayList<>(List.of("Green", "Brown")),
                    new ArrayList<>(List.of("https://example.com/fantasy1.jpg")),
                    "https://example.com/fantasy.schem"),
            new Build("4", "Medieval Bridge", "Author1", "Medieval",
                    "An impressive medieval bridge perfect for river crossings.",
                    new ArrayList<>(List.of("Gray", "Dark Gray")),
                    new ArrayList<>(List.of("https://example.com/medieval_bridge1.jpg")),
                    "https://example.com/medieval_bridge.schem"),
            new Build("5", "Enchanted Tower", "Author2", "Fantasy",
                    "A mystical tower with glowing runes and mysterious vibes.",
                    new ArrayList<>(List.of("Purple", "Blue")),
                    new ArrayList<>(List.of("https://example.com/enchanted_tower1.jpg", "https://example.com/enchanted_tower2.jpg")),
                    "https://example.com/enchanted_tower.schem"),
            new Build("6", "Modern Skyscraper", "Author3", "Modern",
                    "A towering skyscraper with a rooftop garden.",
                    new ArrayList<>(List.of("Silver", "White")),
                    new ArrayList<>(List.of("https://example.com/modern_skyscraper1.jpg", "https://example.com/modern_skyscraper2.jpg")),
                    "https://example.com/modern_skyscraper.schem"),
            new Build("7", "Rustic Cabin", "Author3", "Rustic",
                    "A cozy cabin nestled in the woods.",
                    new ArrayList<>(List.of("Brown", "Green")),
                    new ArrayList<>(List.of("https://example.com/rustic_cabin1.jpg")),
                    "https://example.com/rustic_cabin.schem"),
            new Build("8", "Medieval Watchtower", "Author1", "Medieval",
                    "A sturdy medieval watchtower overlooking the land.",
                    new ArrayList<>(List.of("Gray", "Red")),
                    new ArrayList<>(List.of("https://example.com/watchtower1.jpg", "https://example.com/watchtower2.jpg")),
                    "https://example.com/watchtower.schem"),
            new Build("9", "Fantasy Floating Island", "Author2", "Fantasy",
                    "A magical floating island with waterfalls and lush greenery.",
                    new ArrayList<>(List.of("Blue", "Green")),
                    new ArrayList<>(List.of("https://example.com/floating_island1.jpg", "https://example.com/floating_island2.jpg")),
                    "https://example.com/floating_island.schem"),
            new Build("10", "Modern Beach House", "Author3", "Modern",
                    "A stylish beach house with panoramic ocean views.",
                    new ArrayList<>(List.of("White", "Turquoise")),
                    new ArrayList<>(List.of("https://example.com/beach_house1.jpg")),
                    "https://example.com/beach_house.schem")
    ));

    @Override
    public Optional<Build> findById(String id) {
        return builds.stream()
                .filter(build -> build.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Build> findAll() {
        return builds;
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
                .collect(Collectors.toList());
    }

    @Override
    public List<Build> findByAuthor(String author) {
        return builds.stream()
                .filter(build -> build.getAuthor().equalsIgnoreCase(author))
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
    }
}
