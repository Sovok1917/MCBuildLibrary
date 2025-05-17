package sovok.mcbuildlibrary.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sovok.mcbuildlibrary.model.Build;

public interface BuildRepository extends JpaRepository<Build, Long> {
    
    interface BuildIdAndName {
        Long getId();
        
        String getName();
    }
    
    interface RelatedBuildWithParentId {
        Long getParentId();
        
        Long getBuildId();
        
        String getBuildName();
    }
    
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.id = :id")
    Optional<Build> findByIdWithAssociations(@Param("id") Long id);
    
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.id = :id")
    Optional<Build> findByIdWithAssociationsForLog(@Param("id") Long id);
    
    
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b")
    Page<Build> findAllWithAssociations(Pageable pageable);
    
    /**
     * Filters builds based on provided criteria.
     * Uses LOWER(CAST(column AS string)) for column-side case-insensitivity.
     * Uses LOWER(CONCAT('%', CAST(:parameter AS string), '%')) for parameter-side.
     */
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors ba "
            + "LEFT JOIN FETCH b.themes bt "
            + "LEFT JOIN FETCH b.colors bc "
            + "LEFT JOIN FETCH b.screenshots bs "
            + "WHERE (:authorName IS NULL OR EXISTS (SELECT a FROM b.authors a "
            + "WHERE LOWER(CAST(a.name AS string)) "
            + "LIKE LOWER(CONCAT('%', CAST(:authorName AS string), '%')))) " // CAST param
            + "AND (:buildName IS NULL OR LOWER(CAST(b.name AS string)) "
            + "LIKE LOWER(CONCAT('%', CAST(:buildName AS string), '%'))) "   // CAST param
            + "AND (:themeName IS NULL OR EXISTS (SELECT t FROM b.themes t "
            + "WHERE LOWER(CAST(t.name AS string)) "
            + "LIKE LOWER(CONCAT('%', CAST(:themeName AS string), '%')))) " // CAST param
            + "AND (:colorName IS NULL OR EXISTS (SELECT c FROM b.colors c "
            + "WHERE LOWER(CAST(c.name AS string)) "
            + "LIKE LOWER(CONCAT('%', CAST(:colorName AS string), '%'))))", // CAST param
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b "
                    + "WHERE (:authorName IS NULL OR EXISTS (SELECT a FROM b.authors a "
                    + "WHERE LOWER(CAST(a.name AS string)) "
                    + "LIKE LOWER(CONCAT('%', CAST(:authorName AS string), '%')))) "
                    + "AND (:buildName IS NULL OR LOWER(CAST(b.name AS string)) "
                    + "LIKE LOWER(CONCAT('%', CAST(:buildName AS string), '%'))) "
                    + "AND (:themeName IS NULL OR EXISTS (SELECT t FROM b.themes t "
                    + "WHERE LOWER(CAST(t.name AS string)) "
                    + "LIKE LOWER(CONCAT('%', CAST(:themeName AS string), '%')))) "
                    + "AND (:colorName IS NULL OR EXISTS (SELECT c FROM b.colors c "
                    + "WHERE LOWER(CAST(c.name AS string)) "
                    + "LIKE LOWER(CONCAT('%', CAST(:colorName AS string), '%'))))")
    Page<Build> findFilteredWithAssociations(
            @Param("authorName") String authorName,
            @Param("buildName") String buildName,
            @Param("themeName") String themeName,
            @Param("colorName") String colorName,
            Pageable pageable);
    
    // ... rest of the methods remain the same
    Optional<Build> findByName(String name);
    
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.name = :name")
    Optional<Build> findByNameWithAssociations(@Param("name") String name);
    
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes t "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE t.id = :themeId",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b JOIN b.themes t WHERE t.id = "
                    + ":themeId")
    Page<Build> findBuildsByThemeIdWithAssociations(@Param("themeId") Long themeId,
                                                    Pageable pageable);
    
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors c "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE c.id = :colorId",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b JOIN b.colors c WHERE c.id = "
                    + ":colorId")
    Page<Build> findBuildsByColorIdWithAssociations(@Param("colorId") Long colorId,
                                                    Pageable pageable);
    
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors a "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE a.id = :authorId",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b JOIN b.authors a WHERE a.id = "
                    + ":authorId")
    Page<Build> findBuildsByAuthorIdWithAssociations(@Param("authorId") Long authorId,
                                                     Pageable pageable);
    
    
    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    List<BuildIdAndName> findBuildIdAndNameByAuthorId(@Param("authorId") Long authorId);
    
    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<BuildIdAndName> findBuildIdAndNameByThemeId(@Param("themeId") Long themeId);
    
    @Query("SELECT b.id as id, b.name as name FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<BuildIdAndName> findBuildIdAndNameByColorId(@Param("colorId") Long colorId);
    
    @Query("SELECT a.id as parentId, b.id as buildId, b.name as buildName "
            + "FROM Build b JOIN b.authors a WHERE a.id IN :authorIds")
    List<RelatedBuildWithParentId> findBuildsByAuthorIds(@Param("authorIds") Set<Long> authorIds);
    
    @Query("SELECT t.id as parentId, b.id as buildId, b.name as buildName "
            + "FROM Build b JOIN b.themes t WHERE t.id IN :themeIds")
    List<RelatedBuildWithParentId> findBuildsByThemeIds(@Param("themeIds") Set<Long> themeIds);
    
    @Query("SELECT c.id as parentId, b.id as buildId, b.name as buildName "
            + "FROM Build b JOIN b.colors c WHERE c.id IN :colorIds")
    List<RelatedBuildWithParentId> findBuildsByColorIds(@Param("colorIds") Set<Long> colorIds);
    
    @Query("SELECT b FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<Build> findBuildsByThemeId(@Param("themeId") Long themeId);
    
    @Query("SELECT b FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<Build> findBuildsByColorId(@Param("colorId") Long colorId);
    
    @Query("SELECT b FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    List<Build> findBuildsByAuthorId(@Param("authorId") Long authorId);
}