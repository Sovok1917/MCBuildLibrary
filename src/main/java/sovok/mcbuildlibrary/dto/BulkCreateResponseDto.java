package sovok.mcbuildlibrary.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateResponseDto {

    private List<String> createdAuthors;
    private List<String> skippedAuthors;

    private List<String> createdThemes;
    private List<String> skippedThemes;

    private List<String> createdColors;
    private List<String> skippedColors;
}