package sovok.mcbuildlibrary.repository;

import java.util.Collection; // Import Collection
import java.util.List;
import java.util.Optional;
import java.util.Set; // Import Set
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);

    // Find existing authors by a collection of names (case-insensitive)
    @Query("SELECT a FROM Author a WHERE lower(a.name) IN :names")
    Set<Author> findByNamesIgnoreCase(@Param("names") Collection<String> names);

    @Query(value = "SELECT * FROM author a WHERE :name IS NULL OR SIMILARITY(a.name, :name) > 0.3",
            nativeQuery = true)
    List<Author> fuzzyFindByName(@Param("name") String name);
}