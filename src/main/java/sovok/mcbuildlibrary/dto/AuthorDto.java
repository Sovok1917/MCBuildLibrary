package sovok.mcbuildlibrary.dto;

import java.util.List;

public record AuthorDto(
        Long id,
        String name,
        List<RelatedBuildDto> relatedBuilds
) {
}