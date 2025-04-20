package sovok.mcbuildlibrary.util;

import java.util.List;

/**
 * Holds the result of a bulk creation operation.
 *
 * @param <T> The type of the items created/skipped (e.g., String for names).
 */
public record BulkCreationResult<T>(
        List<T> createdItems,
        List<T> skippedItems
) {
}