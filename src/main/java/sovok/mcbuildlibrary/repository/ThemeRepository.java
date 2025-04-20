package sovok.mcbuildlibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.model.Theme;

@Repository
public interface ThemeRepository extends BaseNamedEntityRepository<Theme> { // Extends BaseNamedEntityRepository

    // findByName and findByNamesIgnoreCase are inherited

    // Native query for fuzzy search remains here due to table name 'theme'
    @Query(value = "SELECT * FROM theme t WHERE :name IS NULL OR SIMILARITY(t.name, :name) > 0.3",
            nativeQuery = true)
    List<Theme> fuzzyFindByName(@Param("name") String name);
}
