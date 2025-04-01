package sovok.mcbuildlibrary.dto;

import java.util.List;

public record ColorDto(
        Long id,
        String name,
        List<RelatedBuildDto> relatedBuilds
) {
}