package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(String name);

    @Query(value = "SELECT * FROM theme t WHERE :name IS NULL OR SIMILARITY(t.name, :name) > 0.3",
            nativeQuery = true)
    List<Theme> fuzzyFindByName(@Param("name") String name);
}