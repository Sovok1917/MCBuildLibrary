package sovok.mcbuildlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToMany;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CascadeType;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Build {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
            name = "build_authors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "author_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST) // Persist authors when build is saved
    private Set<Author> authors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "build_themes",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST) // Persist themes when build is saved
    private Set<Theme> themes = new HashSet<>();

    private String description;

    @ManyToMany
    @JoinTable(
            name = "build_colors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    @org.hibernate.annotations.Cascade(CascadeType.PERSIST) // Persist colors when build is saved
    private Set<Color> colors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "screenshots", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "screenshot")
    @org.hibernate.annotations.Cascade(CascadeType.ALL) // Cascade all operations to screenshots
    private List<String> screenshots;

    @Lob
    @Column(name = "schem_file", columnDefinition = "MEDIUMBLOB")
    @JsonIgnore
    private byte[] schemFile;
}