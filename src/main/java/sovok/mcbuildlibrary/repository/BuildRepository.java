package sovok.mcbuildlibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import sovok.mcbuildlibrary.model.Build;

public interface BuildRepository extends JpaRepository<Build, Long> {

    @Query("SELECT b FROM Build b "
            + "WHERE (:author IS NULL OR EXISTS "
            + "(SELECT a FROM b.authors a WHERE a.name = :author)) "
            + "AND (:name IS NULL OR b.name = :name) "
            + "AND (:theme IS NULL OR EXISTS (SELECT t FROM b.themes t WHERE t.name = :theme)) "
            + "AND (:colorsEmpty = true OR EXISTS "
            + "(SELECT c FROM b.colors c WHERE c.name IN :colors))")
    List<Build> filterBuilds(String author, String name, String theme, List<String> colors,
                             boolean colorsEmpty);
}