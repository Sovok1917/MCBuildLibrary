// file: src/main/java/sovok/mcbuildlibrary/model/Build.java
package sovok.mcbuildlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Set<Author> authors = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "build_themes",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "theme_id")
    )
    private Set<Theme> themes = new HashSet<>();

    private String description;

    @ManyToMany
    @JoinTable(
            name = "build_colors",
            joinColumns = @JoinColumn(name = "build_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    private Set<Color> colors = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "build_screenshots", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "screenshot")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<String> screenshots;

    @Lob
    @Column(name = "schem_file", columnDefinition = "MEDIUMBLOB")
    @JsonIgnore
    private byte[] schemFile;
}