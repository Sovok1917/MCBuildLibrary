package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Build;

public interface BuildRepository extends JpaRepository<Build, Long> {
    @Query("SELECT b FROM Build b "
            + "WHERE (:authorPattern IS NULL OR EXISTS "
            + "(SELECT a FROM b.authors a WHERE LOWER(a.name) LIKE :authorPattern)) "
            + "AND (:namePattern IS NULL OR LOWER(b.name) LIKE :namePattern) "
            + "AND (:themePattern IS NULL OR EXISTS (SELECT t FROM b.themes t WHERE LOWER(t.name)"
            + " LIKE :themePattern)) "
            + "AND (:colorsEmpty = true OR EXISTS "
            + "(SELECT c FROM b.colors c WHERE LOWER(c.name) IN :colorsLower))")
    List<Build> filterBuilds(
            @Param("authorPattern") String authorPattern,
            @Param("namePattern") String namePattern,
            @Param("themePattern") String themePattern,
            @Param("colorsLower") List<String> colorsLower,
            @Param("colorsEmpty") boolean colorsEmpty);

    Optional<Build> findByName(String name);

    // Add this method for partial name matching
    @Query("SELECT b FROM Build b WHERE LOWER(b.name) LIKE :namePattern")
    List<Build> findByNameLike(@Param("namePattern") String namePattern);

    @Query("SELECT b FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<Build> findBuildsByThemeId(@Param("themeId") Long themeId);

    @Query("SELECT b FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<Build> findBuildsByColorId(@Param("colorId") Long colorId);
}