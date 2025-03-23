// file: src/main/java/sovok/mcbuildlibrary/repository/ThemeRepository.java
package sovok.mcbuildlibrary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Theme;

import java.util.Optional;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    Optional<Theme> findByName(String name);
}