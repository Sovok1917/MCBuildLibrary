package sovok.mcbuildlibrary.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Color;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByName(String name);
}