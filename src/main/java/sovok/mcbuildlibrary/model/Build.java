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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import sovok.mcbuildlibrary.exception.StringConstants;
import sovok.mcbuildlibrary.validation.NotPurelyNumeric;

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
    @Size(min = 3, message = StringConstants.NAME_SIZE)
    @Column(unique = true, nullable = false)
    @NotPurelyNumeric
    private String name;

    @NotEmpty(message = "At least one author is required")
    @ManyToMany
    @JoinTable(
            name = "build_authors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @Cascade(CascadeType.PERSIST)
    @Builder.Default
    private Set<Author> authors = new HashSet<>();

    @NotEmpty(message = "At least one theme is required")
    @ManyToMany
    @JoinTable(
            name = "build_themes",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    @Cascade(CascadeType.PERSIST)
    @Builder.Default
    private Set<Theme> themes = new HashSet<>();

    @Size(message = "Description cannot exceed {max} characters")
    private String description;

    @NotEmpty(message = "At least one color is required")
    @ManyToMany
    @JoinTable(
            name = "build_colors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    @Cascade(CascadeType.PERSIST)
    @Builder.Default
    private Set<Color> colors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "screenshots", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "screenshot")
    @Cascade(CascadeType.ALL)
    @Size(max = 10, message = "Maximum of 10 screenshots allowed")


    private List<String> screenshots;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JsonIgnore
    private byte[] schemFile;
}