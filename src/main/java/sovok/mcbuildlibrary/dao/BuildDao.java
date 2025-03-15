package sovok.mcbuildlibrary.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Build;

import java.util.List;

public interface BuildDao extends JpaRepository<Build, Long> {
    @Query("SELECT DISTINCT b FROM Build b LEFT JOIN b.colors c WHERE " +
            "(:author IS NULL OR b.author = :author) AND " +
            "(:name IS NULL OR b.name = :name) AND " +
            "(:theme IS NULL OR b.theme = :theme) AND " +
            "(:colorsEmpty = true OR c IN :colors)")
    List<Build> filterBuilds(@Param("author") String author,
                             @Param("name") String name,
                             @Param("theme") String theme,
                             @Param("colors") List<String> colors,
                             @Param("colorsEmpty") boolean colorsEmpty);
}