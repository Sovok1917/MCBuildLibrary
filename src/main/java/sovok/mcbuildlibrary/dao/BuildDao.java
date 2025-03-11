package sovok.mcbuildlibrary.dao;

import java.util.List;
import java.util.Optional;
import sovok.mcbuildlibrary.model.Build;

public interface BuildDao {
    Optional<Build> findById(String id);

    List<Build> findAll();

    Optional<Build> findByName(String name);

    List<Build> findByTheme(String theme);

    List<Build> findByAuthor(String author);

    List<Build> filterBuilds(String author, String name, String theme, List<String> colors);
}
