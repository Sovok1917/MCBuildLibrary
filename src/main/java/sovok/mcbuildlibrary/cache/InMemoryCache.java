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

@Component
public class InMemoryCache {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryCache.class);
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    @Getter
    private final int maxSize;

    private static final String KEY_SEPARATOR = "::";
    private static final String QUERY_KEY_PREFIX = "QUERY";
    private static final String NULL_PARAM_PLACEHOLDER = "__NULL__";
    private static final String PARAM_SEPARATOR = "&";
    private static final String LIST_ELEMENT_SEPARATOR = "|";

    public InMemoryCache(@Value("${cache.max.size:1000}") int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("Cache max size must be positive.");
        }
        this.maxSize = maxSize;
        logger.info("Initialized InMemoryCache with max size before clear: {}", this.maxSize);
    }


    public static String generateKey(String entityType, Object identifier) {
        return entityType + KEY_SEPARATOR + identifier;
    }

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



    public void put(String key, Object value) {
        if (key == null || value == null) {
            logger.warn("Attempted to put null key or value into cache. Key: {}", key);
            return;
        }


        synchronized (cache) {


            if (cache.size() >= maxSize && !cache.containsKey(key)) {
                logger.warn("Cache limit ({}) reached. Clearing entire cache before adding key: {}",
                        maxSize, key);
                cache.clear();
            }

            logger.debug("Caching item with key: {}", key);
            cache.put(key, value);
        }
    }



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
                evict(key);
                return Optional.empty();
            }
        } else {
            logger.debug("Cache miss for key: {}", key);
            return Optional.empty();
        }
    }

    public void evict(String key) {
        if (key != null) {
            logger.debug("Evicting specific item with key: {}", key);
            cache.remove(key);
        }
    }

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