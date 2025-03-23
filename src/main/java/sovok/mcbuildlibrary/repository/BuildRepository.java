package sovok.mcbuildlibrary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.model.Build;

import java.util.List;

public interface BuildRepository extends JpaRepository<Build, Long> {

    @Query("SELECT b FROM Build b " +
            "WHERE (:author IS NULL OR EXISTS (SELECT a FROM b.authors a WHERE a.name = :author)) " +
            "AND (:name IS NULL OR b.name = :name) " +
            "AND (:theme IS NULL OR b.theme = :theme) " +
            "AND (:colorsEmpty = true OR EXISTS (SELECT c FROM b.colors c WHERE c IN :colors))")
    List<Build> filterBuilds(String author, String name, String theme, List<String> colors, boolean colorsEmpty);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM build_screenshots WHERE build_id = :buildId", nativeQuery = true)
    void deleteScreenshotsByBuildId(Long buildId);

    @Query("SELECT COUNT(b) FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    long countByAuthorsId(Long authorId);
}