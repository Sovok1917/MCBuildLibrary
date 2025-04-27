package sovok.mcbuildlibrary.util;

import java.util.List;

public record BulkCreationResult<T>(
        List<T> createdItems,
        List<T> skippedItems
) {
}