package sovok.mcbuildlibrary.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import sovok.mcbuildlibrary.model.Build;

import java.util.List;

public interface BuildDao extends JpaRepository<Build, Long> {

    @Query("SELECT b FROM Build b " +
            "WHERE (:author IS NULL OR b.author.name = :author) " +
            "AND (:name IS NULL OR b.name = :name) " +
            "AND (:theme IS NULL OR b.theme = :theme) " +
            "AND (:colorsEmpty = true OR EXISTS (SELECT c FROM b.colors c WHERE c IN :colors))")
    List<Build> filterBuilds(String author, String name, String theme, List<String> colors, boolean colorsEmpty);

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM build_screenshots WHERE build_id = :buildId", nativeQuery = true)
    void deleteScreenshotsByBuildId(Long buildId);

    long countByAuthorId(Long authorId);
}