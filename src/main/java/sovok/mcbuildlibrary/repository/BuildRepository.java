package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Build;

public interface BuildRepository extends JpaRepository<Build, Long> {

    interface BuildIdAndName {
        Long getId();

        String getName();
    }

    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.id = :id")
    Optional<Build> findByIdWithAssociationsForLog(@Param("id") Long id);

    @Query(value = "SELECT DISTINCT b.* FROM build b "
            + "LEFT JOIN build_authors ba ON b.id = ba.build_id "
            + "LEFT JOIN author a ON ba.author_id = a.id "
            + "LEFT JOIN build_themes bt ON b.id = bt.build_id "
            + "LEFT JOIN theme t ON bt.theme_id = t.id "
            + "LEFT JOIN build_colors bc ON b.id = bc.build_id "
            + "LEFT JOIN color c ON bc.color_id = c.id "
            + "WHERE (:author IS NULL OR SIMILARITY(a.name, :author) > 0.3) "
            + "AND (:name IS NULL OR SIMILARITY(b.name, :name) > 0.3) "
            + "AND (:theme IS NULL OR SIMILARITY(t.name, :theme) > 0.3) "
            + "AND (:color IS NULL OR SIMILARITY(c.name, :color) > 0.3) "
            + "GROUP BY b.id",
            nativeQuery = true)
    List<Build> fuzzyFilterBuilds(
            @Param("author") String author,
            @Param("name") String name,
            @Param("theme") String theme,
            @Param("color") String color);

    Optional<Build> findByName(String name);

    @Query("SELECT b FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<Build> findBuildsByThemeId(@Param("themeId") Long themeId);

    @Query("SELECT b FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<Build> findBuildsByColorId(@Param("colorId") Long colorId);

    @Query("SELECT b FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    List<Build> findBuildsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    List<BuildIdAndName> findBuildIdAndNameByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<BuildIdAndName> findBuildIdAndNameByThemeId(@Param("themeId") Long themeId);

    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<BuildIdAndName> findBuildIdAndNameByColorId(@Param("colorId") Long colorId);
}