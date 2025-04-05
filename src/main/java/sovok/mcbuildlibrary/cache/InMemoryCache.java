package sovok.mcbuildlibrary.cache;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A simple thread-safe in-memory cache implementation using ConcurrentHashMap.
 * Handles single items, "getAll" lists, and query results.
 */
@Component
public class InMemoryCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCache.class);
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    private static final String KEY_SEPARATOR = "::";
    private static final String GET_ALL_KEY_SUFFIX = "ALL";
    private static final String QUERY_KEY_PREFIX = "QUERY"; // Prefix for query keys
    private static final String NULL_PARAM_PLACEHOLDER = "__NULL__";
    private static final String PARAM_SEPARATOR = "&"; // Separator between params in key
    private static final String LIST_ELEMENT_SEPARATOR = "|"; // Separator for list elements in key

    /**
     * Generates a cache key for a specific entity instance.
     * e.g., "Author::123"
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     * @param identifier The unique identifier (e.g., ID or unique Name).
     * @return The generated cache key.
     */
    public static String generateKey(String entityType, Object identifier) {
        return entityType + KEY_SEPARATOR + identifier;
    }

    /**
     * Generates a cache key for retrieving all entities of a specific type.
     * e.g., "Author::ALL"
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     * @return The generated cache key for "get all".
     */
    public static String generateGetAllKey(String entityType) {
        return entityType + KEY_SEPARATOR + GET_ALL_KEY_SUFFIX;
    }

    /**
     * Generates a cache key for a query based on its parameters.
     * Ensures consistent key generation regardless of parameter order.
     * Handles nulls and lists.
     * e.g., "Build::QUERY::author=Sovok&colors=Blue|Red&name=__NULL__&theme=Medieval"
     *
     * @param entityType The type of the entity being queried (e.g., "Author", "Build").
     * @param queryParams A map of query parameter names to their values.
     * @return The generated cache key for the query.
     */
    public static String generateQueryKey(String entityType, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            // Handle case with no parameters - could be like getAll or a specific query endpoint
            return entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR + "noparams";
        }

        String sortedParamsString = queryParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Sort by key for consistency
                .map(entry -> entry.getKey() + "="
                        + formatParamValueForKey(entry.getValue()))
                .collect(Collectors.joining(PARAM_SEPARATOR));

        return entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR + sortedParamsString;
    }

    /**
     * Formats a parameter value for inclusion in a cache key.
     * Handles nulls and lists consistently.
     */
    private static String formatParamValueForKey(Object value) {
        if (value == null) {
            return NULL_PARAM_PLACEHOLDER;
        }
        if (value instanceof List<?> listValue) {
            if (listValue.isEmpty()) {
                return ""; // Empty list representation
            }
            // Sort list elements (as strings) for consistency
            return listValue.stream()
                    .map(Objects::toString)
                    .sorted()
                    .collect(Collectors.joining(LIST_ELEMENT_SEPARATOR));
        }
        // For simple types, toString is usually sufficient
        return value.toString();
    }


    /**
     * Puts an item into the cache.
     *
     * @param key   The cache key.
     * @param value The value to cache.
     */
    public void put(String key, Object value) {
        if (key == null || value == null) {
            logger.warn("Attempted to put null key or value into cache. Key: {}", key);
            return;
        }
        logger.debug("Caching item with key: {}", key);
        cache.put(key, value);
    }

    /**
     * Retrieves an item from the cache.
     *
     * @param key The cache key.
     * @return An Optional containing the cached value if found, otherwise empty.
     */
    @SuppressWarnings("unchecked") // We expect the caller to know the type
    public <T> Optional<T> get(String key) {
        Object value = cache.get(key);
        if (value != null) {
            logger.debug("Cache hit for key: {}", key);
            try {
                return Optional.of((T) value);
            } catch (ClassCastException e) {
                logger.error("Cache type mismatch for key: {}. Expected type not found. "
                        + "Evicting entry.", key, e);
                evict(key); // Invalidate potentially corrupt entry
                return Optional.empty();
            }
        } else {
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        }
    }

    /**
     * Removes an item from the cache.
     *
     * @param key The cache key to evict.
     */
    public void evict(String key) {
        if (key != null) {
            logger.debug("Evicting item with key: {}", key);
            cache.remove(key);
        }
    }

    /**
     * Removes all *query* cache entries related to a specific entity type.
     * Useful for invalidating search results when underlying data changes.
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     */
    public void evictQueryCacheByType(String entityType) {
        String queryPrefix = entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR;
        logger.debug("Evicting query cache entries for type: {}", entityType);
        cache.keySet().removeIf(key -> key.startsWith(queryPrefix));
    }
}