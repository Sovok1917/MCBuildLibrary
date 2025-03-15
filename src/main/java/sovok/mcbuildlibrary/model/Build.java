package sovok.mcbuildlibrary.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @NotBlank(message = "Name is mandatory")
    private String name;

    @NotBlank(message = "Author is mandatory")
    private String author;

    @NotBlank(message = "Theme is mandatory")
    private String theme;

    private String description;

    @NotEmpty(message = "At least one color is required")
    @ElementCollection
    private List<String> colors;

    @ElementCollection
    private List<String> screenshots;

    @Lob
    @Column(name = "schem_file", columnDefinition = "MEDIUMBLOB", nullable = false)
    private byte[] schemFile;
}