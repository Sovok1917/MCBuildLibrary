package sovok.mcbuildlibrary.repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.model.Author;

@Repository

public interface AuthorRepository extends BaseNamedEntityRepository<Author> {
    @Query(value = "SELECT * FROM author a WHERE :name IS NULL OR SIMILARITY(a.name, :name) > 0.3",
            nativeQuery = true)
    List<Author> fuzzyFindByName(@Param("name") String name);
}