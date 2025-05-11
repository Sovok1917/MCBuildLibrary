package sovok.mcbuildlibrary.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sovok.mcbuildlibrary.model.User;

/**
 * Repository interface for {@link User} entities.
 * Provides methods to retrieve user data from the database.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
   * Finds a user by their username.
   *
   * @param username The username to search for.
   * @return An {@link Optional} containing the user if found, or empty otherwise.
   */
    Optional<User> findByUsername(String username);
}