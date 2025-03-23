// file: src/main/java/sovok/mcbuildlibrary/repository/ColorRepository.java
package sovok.mcbuildlibrary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sovok.mcbuildlibrary.model.Color;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {
    Optional<Color> findByName(String name);
}