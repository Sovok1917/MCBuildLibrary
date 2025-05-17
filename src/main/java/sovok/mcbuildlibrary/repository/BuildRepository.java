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

/**
 * Repository for {@link Build} entities.
 * Provides methods for CRUD operations and custom queries,
 * including fetching builds with their associations and pagination.
 */
public interface BuildRepository extends JpaRepository<Build, Long> {
    
    /**
     * Interface for projecting Build ID and Name.
     * Used for optimized fetching of related build information.
     */
    interface BuildIdAndName {
        Long getId();
        
        String getName();
    }
    
    /**
     * Interface for mapping related builds with their parent entity ID.
     * Used in bulk fetching operations for DTO conversion.
     */
    interface RelatedBuildWithParentId {
        Long getParentId(); // AuthorId, ThemeId, or ColorId
        
        Long getBuildId();
        
        String getBuildName();
    }
    
    /**
     * Finds a build by its ID, eagerly fetching all its associations.
     * This is primarily used for detailed views or operations like log generation
     * where all data is needed.
     *
     * @param id The ID of the build.
     * @return An {@link Optional} containing the build with associations if found.
     */
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.id = :id")
    Optional<Build> findByIdWithAssociations(@Param("id") Long id);
    
    /**
     * Finds a build by its ID, eagerly fetching associations needed for log generation.
     *
     * @param id The ID of the build.
     * @return An {@link Optional} containing the build with associations if found.
     */
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.id = :id")
    Optional<Build> findByIdWithAssociationsForLog(@Param("id") Long id);
    
    
    /**
     * Finds all builds with their associations, supporting pagination.
     * Collections (authors, themes, colors, screenshots) are eagerly fetched.
     *
     * @param pageable Pagination information.
     * @return A {@link Page} of builds.
     */
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b")
    Page<Build> findAllWithAssociations(Pageable pageable);
    
    /**
     * Filters builds based on provided criteria (author name, build name, theme name, color name)
     * using case-insensitive LIKE comparisons, with associations eagerly fetched and pagination.
     *
     * @param authorName Optional author name to filter by (fuzzy match).
     * @param buildName  Optional build name to filter by (fuzzy match).
     * @param themeName  Optional theme name to filter by (fuzzy match).
     * @param colorName  Optional color name to filter by (fuzzy match).
     * @param pageable   Pagination information.
     * @return A {@link Page} of filtered builds.
     */
    @Query(value = "SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors ba "
            + "LEFT JOIN FETCH b.themes bt "
            + "LEFT JOIN FETCH b.colors bc "
            + "LEFT JOIN FETCH b.screenshots bs "
            + "WHERE (:authorName IS NULL OR EXISTS (SELECT a FROM b.authors a WHERE LOWER(a.name) "
            + "LIKE LOWER(CONCAT('%', :authorName, '%')))) "
            + "AND (:buildName IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :buildName, '%'))) "
            + "AND (:themeName IS NULL OR EXISTS (SELECT t FROM b.themes t WHERE LOWER(t.name) "
            + "LIKE LOWER(CONCAT('%', :themeName, '%')))) "
            + "AND (:colorName IS NULL OR EXISTS (SELECT c FROM b.colors c WHERE LOWER(c.name) "
            + "LIKE LOWER(CONCAT('%', :colorName, '%'))))",
            countQuery = "SELECT COUNT(DISTINCT b) FROM Build b "
                    + "WHERE (:authorName IS NULL OR EXISTS (SELECT a FROM b.authors "
                    + "a WHERE LOWER(a.name) "
                    + "LIKE LOWER(CONCAT('%', :authorName, '%')))) "
                    + "AND (:buildName IS NULL OR LOWER(b.name) LIKE "
                    + "LOWER(CONCAT('%', :buildName, '%'))) "
                    + "AND (:themeName IS NULL OR EXISTS (SELECT t FROM b.themes t "
                    + "WHERE LOWER(t.name) "
                    + "LIKE LOWER(CONCAT('%', :themeName, '%')))) "
                    + "AND (:colorName IS NULL OR EXISTS (SELECT c FROM b.colors c "
                    + "WHERE LOWER(c.name) "
                    + "LIKE LOWER(CONCAT('%', :colorName, '%'))))")
    Page<Build> findFilteredWithAssociations(
            @Param("authorName") String authorName,
            @Param("buildName") String buildName,
            @Param("themeName") String themeName,
            @Param("colorName") String colorName,
            Pageable pageable);
    
    /**
     * Finds a build by its exact name.
     *
     * @param name The name of the build.
     * @return An {@link Optional} containing the build if found.
     */
    Optional<Build> findByName(String name);
    
    /**
     * Finds a build by its exact name, eagerly fetching associations.
     *
     * @param name The name of the build.
     * @return An {@link Optional} containing the build with associations if found.
     */
    @Query("SELECT DISTINCT b FROM Build b "
            + "LEFT JOIN FETCH b.authors "
            + "LEFT JOIN FETCH b.themes "
            + "LEFT JOIN FETCH b.colors "
            + "LEFT JOIN FETCH b.screenshots "
            + "WHERE b.name = :name")
    Optional<Build> findByNameWithAssociations(@Param("name") String name);
    
    /**
     * Finds builds associated with a specific theme ID, with associations eagerly fetched and
     * pagination.
     *
     * @param themeId  The ID of the theme.
     * @param pageable Pagination information.
     * @return A {@link Page} of builds associated with the theme.
     */
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
    
    /**
     * Finds builds associated with a specific color ID, with associations eagerly fetched and
     * pagination.
     *
     * @param colorId  The ID of the color.
     * @param pageable Pagination information.
     * @return A {@link Page} of builds associated with the color.
     */
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
    
    /**
     * Finds builds associated with a specific author ID, with associations eagerly fetched and
     * pagination.
     *
     * @param authorId The ID of the author.
     * @param pageable Pagination information.
     * @return A {@link Page} of builds associated with the author.
     */
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
    
    
    // Methods for DTO conversion (related builds for Author/Theme/Color DTOs)
    // These are used by BaseNamedEntityService and its children.
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
    
    // Legacy methods (to be reviewed if still needed or can be replaced by paginated versions)
    @Query("SELECT b FROM Build b JOIN b.themes t WHERE t.id = :themeId")
    List<Build> findBuildsByThemeId(@Param("themeId") Long themeId);
    
    @Query("SELECT b FROM Build b JOIN b.colors c WHERE c.id = :colorId")
    List<Build> findBuildsByColorId(@Param("colorId") Long colorId);
    
    @Query("SELECT b FROM Build b JOIN b.authors a WHERE a.id = :authorId")
    List<Build> findBuildsByAuthorId(@Param("authorId") Long authorId);
}