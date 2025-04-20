package sovok.mcbuildlibrary.repository;

import java.util.Collection; // Import Collection
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Color;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByName(String name);

    // Find existing colors by a collection of names (case-insensitive)
    @Query("SELECT c FROM Color c WHERE lower(c.name) IN :names")
    Set<Color> findByNamesIgnoreCase(@Param("names") Collection<String> names);

    @Query(value = "SELECT * FROM color c WHERE :name IS NULL OR SIMILARITY(c.name, :name) > 0.3",
            nativeQuery = true)
    List<Color> fuzzyFindByName(@Param("name") String name);
}