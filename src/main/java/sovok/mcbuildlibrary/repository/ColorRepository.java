package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Color;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByName(String name);

    @Query(value = "SELECT * FROM color c WHERE :name IS NULL OR SIMILARITY(c.name, :name) > 0.3",
            nativeQuery = true)
    List<Color> fuzzyFindByName(@Param("name") String name);
}