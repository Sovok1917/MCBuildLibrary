package sovok.mcbuildlibrary.cache;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A simple thread-safe in-memory cache implementation using ConcurrentHashMap.
 * Handles single items, "getAll" lists, and query results.
 * Includes a maximum size limit. When the limit is reached and a new item is added,
 * the entire cache is cleared to maintain accuracy.
 */
@Component
public class InMemoryCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCache.class);
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    /**
     * -- GETTER --
     *  Gets the configured maximum size threshold that triggers a clear.
     *
     */
    @Getter
    private final int maxSize; // Maximum size of the cache

    private static final String KEY_SEPARATOR = "::";
    private static final String QUERY_KEY_PREFIX = "QUERY";
    private static final String NULL_PARAM_PLACEHOLDER = "__NULL__";
    private static final String PARAM_SEPARATOR = "&";
    private static final String LIST_ELEMENT_SEPARATOR = "|";

    /**
     * Constructor to initialize the cache with a maximum size.
     * The maximum size is injected from application properties (e.g., cache.max.size)
     * with a default value of 1000.
     *
     * @param maxSize The maximum number of entries the cache can hold before being cleared.
     */
    public InMemoryCache(@Value("${cache.max.size:1000}") int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Cache max size must be positive.");
        }
        this.maxSize = maxSize;
        logger.info("Initialized InMemoryCache with max size before clear: {}", this.maxSize);
    }


    // --- Key Generation Methods (Unchanged) ---

    /**
     * Generates a cache key for a specific entity instance.
     * e.g., "Author::123"
     */
    public static String generateKey(String entityType, Object identifier) {
        return entityType + KEY_SEPARATOR + identifier;
    }

    /**
     * Generates a cache key for a query based on its parameters.
     * e.g., "Build::QUERY::author=Sovok&colors=Blue|Red&name=__NULL__&theme=Medieval"
     */
    public static String generateQueryKey(String entityType, Map<String, Object> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR + "noparams";
        }

        String sortedParamsString = queryParams.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "="
                        + formatParamValueForKey(entry.getValue()))
                .collect(Collectors.joining(PARAM_SEPARATOR));

        return entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR + sortedParamsString;
    }

    /**
     * Formats a parameter value for inclusion in a cache key.
     */
    private static String formatParamValueForKey(Object value) {
        if (value == null) {
            return NULL_PARAM_PLACEHOLDER;
        }
        if (value instanceof List<?> listValue) {
            if (listValue.isEmpty()) {
                return "";
            }
            return listValue.stream()
                    .map(Objects::toString)
                    .sorted()
                    .collect(Collectors.joining(LIST_ELEMENT_SEPARATOR));
        }
        return value.toString();
    }


    // --- Cache Operations ---

    /**
     * Puts an item into the cache.
     * If the cache is full (reaches maxSize) and the key is new,
     * the *entire cache is cleared* before adding the new one to ensure accuracy.
     *
     * @param key   The cache key.
     * @param value The value to cache.
     */
    public void put(String key, Object value) {
        if (key == null || value == null) {
            logger.warn("Attempted to put null key or value into cache. Key: {}", key);
            return;
        }

        // Synchronize the check-and-clear logic
        synchronized (cache) {
            // Check if clearing is needed *before* putting the new item
            // Only clear if the cache is full AND we are adding a *new* key
            if (cache.size() >= maxSize && !cache.containsKey(key)) {
                logger.warn("Cache limit ({}) reached. Clearing entire cache before adding key: {}",
                        maxSize, key);
                cache.clear(); // Clear the entire cache
            }
            // Now put the item
            logger.debug("Caching item with key: {}", key);
            cache.put(key, value);
        }
    }

    // Removed the evictOldestEntry() method as it's replaced by cache.clear()

    /**
     * Retrieves an item from the cache.
     *
     * @param key The cache key.
     * @return An Optional containing the cached value if found, otherwise empty.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        Object value = cache.get(key);
        if (value != null) {
            logger.debug("Cache hit for key: {}", key);
            try {
                return Optional.of((T) value);
            } catch (ClassCastException e) {
                logger.error("Cache type mismatch for key: {}. Expected type not found. "
                        + "Evicting entry.", key, e);
                evict(key); // Still evict potentially corrupt entries on type mismatch
                return Optional.empty();
            }
        } else {
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        }
    }

    /**
     * Removes a specific item from the cache.
     *
     * @param key The cache key to evict.
     */
    public void evict(String key) {
        if (key != null) {
            logger.debug("Evicting specific item with key: {}", key);
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
        if (entityType == null) {
            return;
        }

        String queryPrefix = entityType + KEY_SEPARATOR + QUERY_KEY_PREFIX + KEY_SEPARATOR;
        logger.debug("Evicting query cache entries for type starting with: {}", queryPrefix);

        cache.keySet().removeIf(key -> key != null && key.startsWith(queryPrefix));
        logger.debug("Finished evicting query cache entries for type: {}", entityType);
    }
}