package sovok.mcbuildlibrary.repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.BaseNamedEntity;


@NoRepositoryBean

public interface BaseNamedEntityRepository<T extends BaseNamedEntity> extends JpaRepository
        <T, Long> {

    Optional<T> findByName(String name);

    @Query("SELECT e FROM #{#entityName} e WHERE lower(e.name) IN :names")
    Set<T> findByNamesIgnoreCase(@Param("names") Collection<String> names);
}