package sovok.mcbuildlibrary.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

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

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Author author;

    private String theme;

    private String description;

    @ElementCollection
    @CollectionTable(name = "build_colors", joinColumns = @JoinColumn(name = "build_id"))
    @Column(name = "color")
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<String> colors;

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