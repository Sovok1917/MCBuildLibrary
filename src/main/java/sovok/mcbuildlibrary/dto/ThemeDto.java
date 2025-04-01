package sovok.mcbuildlibrary.dto;

import java.util.List;

public record ThemeDto(
        Long id,
        String name,
        List<RelatedBuildDto> relatedBuilds
) {
}