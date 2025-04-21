package sovok.mcbuildlibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.model.Author;

@Repository // Add @Repository for clarity, though not strictly necessary
// if component scanning is broad
public interface AuthorRepository extends BaseNamedEntityRepository<Author> {
    // Extends BaseNamedEntityRepository

    // findByName and findByNamesIgnoreCase are inherited

    // Native query for fuzzy search remains here due to table name 'author'
    @Query(value = "SELECT * FROM author a WHERE :name IS NULL OR SIMILARITY(a.name, :name) > 0.3",
            nativeQuery = true)
    List<Author> fuzzyFindByName(@Param("name") String name);
}