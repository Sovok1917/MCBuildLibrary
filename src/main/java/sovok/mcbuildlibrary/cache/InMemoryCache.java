// file: src/main/java/sovok/mcbuildlibrary/cache/InMemoryCache.java
package sovok.mcbuildlibrary.cache;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * A simple thread-safe in-memory cache implementation using ConcurrentHashMap.
 */
@Component
public class InMemoryCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCache.class);
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    private static final String KEY_SEPARATOR = "::";
    private static final String GET_ALL_KEY_SUFFIX = "ALL";

    /**
     * Generates a cache key for a specific entity.
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     * @param identifier The unique identifier (e.g., ID).
     * @return The generated cache key.
     */
    public static String generateKey(String entityType, Object identifier) {
        return entityType + KEY_SEPARATOR + identifier;
    }

    /**
     * Generates a cache key for retrieving all entities of a specific type.
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     * @return The generated cache key for "get all".
     */
    public static String generateGetAllKey(String entityType) {
        return entityType + KEY_SEPARATOR + GET_ALL_KEY_SUFFIX;
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
                logger.error("Cache type mismatch for key: {}. Expected type not found.", key, e);
                // Invalidate potentially corrupt entry
                evict(key);
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
     * Clears the entire cache.
     */
    public void clear() {
        logger.info("Clearing entire cache.");
        cache.clear();
    }

    /**
     * Removes all entries related to a specific entity type (both individual and "getAll").
     * This is a simple implementation; more complex scenarios might need different logic.
     *
     * @param entityType The type of the entity (e.g., "Author", "Build").
     */
    public void evictByType(String entityType) {
        String prefix = entityType + KEY_SEPARATOR;
        logger.debug("Evicting all entries for type: {}", entityType);
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }
}