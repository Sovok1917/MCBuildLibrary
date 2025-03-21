package sovok.mcbuildlibrary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Author;

import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);
}