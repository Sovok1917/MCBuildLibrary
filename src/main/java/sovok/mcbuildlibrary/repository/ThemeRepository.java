package sovok.mcbuildlibrary.repository;

import java.util.Collection; // Import Collection
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(String name);

    // Find existing themes by a collection of names (case-insensitive)
    @Query("SELECT t FROM Theme t WHERE lower(t.name) IN :names")
    Set<Theme> findByNamesIgnoreCase(@Param("names") Collection<String> names);

    @Query(value = "SELECT * FROM theme t WHERE :name IS NULL OR SIMILARITY(t.name, :name) > 0.3",
            nativeQuery = true)
    List<Theme> fuzzyFindByName(@Param("name") String name);
}