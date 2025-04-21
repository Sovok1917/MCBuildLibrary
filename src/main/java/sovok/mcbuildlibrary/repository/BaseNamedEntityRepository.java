package sovok.mcbuildlibrary.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.BaseNamedEntity;


/**
 * Base repository interface for entities extending BaseNamedEntity.
 * Provides common query methods.
 * Annotated with @NoRepositoryBean so Spring Data doesn't try to create an instance of it. [2, 6]
 *
 * @param <T> The specific entity type extending BaseNamedEntity.
 */
@NoRepositoryBean // VERY IMPORTANT! Prevents Spring Data from creating a bean for this
// interface. [6]
public interface BaseNamedEntityRepository<T extends BaseNamedEntity> extends JpaRepository
        <T, Long> {

    /**
     * Finds an entity by its exact name (case-sensitive).
     *
     * @param name The name to search for.
     * @return An Optional containing the entity if found, otherwise empty.
     */
    Optional<T> findByName(String name);

    /**
     * Finds existing entities by a collection of names (case-insensitive).
     * Useful for bulk operations.
     *
     * @param names A collection of names to search for.
     * @return A Set containing the found entities.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE lower(e.name) IN :names")
    Set<T> findByNamesIgnoreCase(@Param("names") Collection<String> names);

    // Note: fuzzyFindByName using native SIMILARITY cannot be cleanly put here
    // because the table name varies. It will remain in the concrete repositories.
}