package sovok.mcbuildlibrary.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Build {
    private String id;
    private String name;
    private String author;
    private String theme;
    private String description;
    private List<String> colors;
    private List<String> screenshots;
    private String schemFilePaths;
}
