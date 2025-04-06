// file: src/main/java/sovok/mcbuildlibrary/model/Build.java
package sovok.mcbuildlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank; // Import
import jakarta.validation.constraints.NotEmpty; // Import for collections
import jakarta.validation.constraints.NotNull; // Import for objects
import jakarta.validation.constraints.Size;     // Import
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CascadeType;
import sovok.mcbuildlibrary.exception.StringConstants; // Import

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Build {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = StringConstants.NAME_NOT_BLANK)
    // Remove max = 100, keep min = 3 if desired
    @Size(min = 3, message = StringConstants.NAME_SIZE)
    @Column(unique = true, nullable = false)
    private String name;

    @NotEmpty(message = "At least one author is required")
    @ManyToMany
    @JoinTable(
            name = "build_authors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST)
    private Set<Author> authors = new HashSet<>();

    @NotEmpty(message = "At least one theme is required")
    @ManyToMany
    @JoinTable(
            name = "build_themes",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST)
    private Set<Theme> themes = new HashSet<>();

    // Remove max = 500 from description
    @Size(message = "Description cannot exceed {max} characters") // Keep message generic if needed, but remove max
    private String description;

    @NotEmpty(message = "At least one color is required")
    @ManyToMany
    @JoinTable(
            name = "build_colors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST)
    private Set<Color> colors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "screenshots", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "screenshot")
    @org.hibernate.annotations.Cascade(CascadeType.ALL)
    @Size(max = 10, message = "Maximum of 10 screenshots allowed") // This is usually fine as it doesn't alter a core column type
    private List<String> screenshots;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] schemFile;
}