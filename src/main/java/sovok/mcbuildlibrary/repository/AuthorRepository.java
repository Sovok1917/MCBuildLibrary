package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);

    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE :pattern")
    List<Author> findByNameLike(@Param("pattern") String pattern);

    @Query(value = "SELECT * FROM author a WHERE :name IS NULL OR SIMILARITY(a.name, :name) > 0.3",
            nativeQuery = true)
    List<Author> fuzzyFindByName(@Param("name") String name);
}