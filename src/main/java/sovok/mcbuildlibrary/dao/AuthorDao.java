package sovok.mcbuildlibrary.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Author;

import java.util.Optional;

public interface AuthorDao extends JpaRepository<Author, Long> {
    Optional<Author> findByName(String name);
}