package sovok.mcbuildlibrary.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Theme;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(String name);
}