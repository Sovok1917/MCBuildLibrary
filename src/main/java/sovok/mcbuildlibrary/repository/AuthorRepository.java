package sovok.mcbuildlibrary.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Author;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);
}