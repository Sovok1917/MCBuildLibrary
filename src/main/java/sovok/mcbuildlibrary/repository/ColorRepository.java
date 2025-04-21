package sovok.mcbuildlibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.model.Color;

@Repository
public interface ColorRepository extends BaseNamedEntityRepository<Color> {
    // Extends BaseNamedEntityRepository

    // findByName and findByNamesIgnoreCase are inherited

    // Native query for fuzzy search remains here due to table name 'color'
    @Query(value = "SELECT * FROM color c WHERE :name IS NULL OR SIMILARITY(c.name, :name) > 0.3",
            nativeQuery = true)
    List<Color> fuzzyFindByName(@Param("name") String name);
}