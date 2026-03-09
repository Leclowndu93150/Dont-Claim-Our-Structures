package com.leclowndu93150.dont_touch_our_structures.protection;

import com.leclowndu93150.dont_touch_our_structures.config.ModConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.LinkedHashMap;
import java.util.Map;

public class StructureCache {

    private record CacheEntry(ProtectionResult result, long timestamp) {
        boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - timestamp > ttlMillis;
        }
    }

    private record CacheKey(ResourceLocation dimension, int chunkX, int chunkZ) {
        static CacheKey of(ResourceLocation dimension, ChunkPos pos) {
            return new CacheKey(dimension, pos.x, pos.z);
        }
    }

    private final Map<CacheKey, CacheEntry> cache;
    private final Object lock = new Object();
    private long hits = 0;
    private long misses = 0;
    private long evictions = 0;

    public StructureCache() {
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                boolean shouldRemove = size() > ModConfig.cacheMaxEntries;
                if (shouldRemove) {
                    evictions++;
                }
                return shouldRemove;
            }
        };
    }

    public ProtectionResult get(ResourceLocation dimension, ChunkPos chunkPos) {
        if (!ModConfig.enableCache) {
            return null;
        }

        CacheKey key = CacheKey.of(dimension, chunkPos);
        long ttlMillis = ModConfig.cacheTtlSeconds * 1000L;

        synchronized (lock) {
            CacheEntry entry = cache.get(key);
            if (entry == null) {
                misses++;
                return null;
            }

            if (entry.isExpired(ttlMillis)) {
                cache.remove(key);
                misses++;
                return null;
            }

            hits++;
            return entry.result();
        }
    }

    public void put(ResourceLocation dimension, ChunkPos chunkPos, ProtectionResult result) {
        if (!ModConfig.enableCache) {
            return;
        }

        CacheKey key = CacheKey.of(dimension, chunkPos);

        synchronized (lock) {
            cache.put(key, new CacheEntry(result, System.currentTimeMillis()));
        }
    }

    public void invalidate(ResourceLocation dimension, ChunkPos chunkPos) {
        CacheKey key = CacheKey.of(dimension, chunkPos);
        synchronized (lock) {
            cache.remove(key);
        }
    }

    public void invalidateDimension(ResourceLocation dimension) {
        synchronized (lock) {
            cache.keySet().removeIf(key -> key.dimension().equals(dimension));
        }
    }

    public void clear() {
        synchronized (lock) {
            cache.clear();
            hits = 0;
            misses = 0;
            evictions = 0;
        }
    }

    public void cleanupExpired() {
        long ttlMillis = ModConfig.cacheTtlSeconds * 1000L;
        synchronized (lock) {
            cache.entrySet().removeIf(entry -> entry.getValue().isExpired(ttlMillis));
        }
    }

    public int size() {
        synchronized (lock) {
            return cache.size();
        }
    }

    public String getStats() {
        synchronized (lock) {
            long total = hits + misses;
            double hitRate = total > 0 ? (hits * 100.0 / total) : 0;
            return String.format(
                    "Cache Stats - Size: %d/%d, Hits: %d, Misses: %d, Evictions: %d, Hit Rate: %.1f%%",
                    cache.size(), ModConfig.cacheMaxEntries, hits, misses, evictions, hitRate
            );
        }
    }

    public long getHits() {
        synchronized (lock) {
            return hits;
        }
    }

    public long getMisses() {
        synchronized (lock) {
            return misses;
        }
    }

    public long getEvictions() {
        synchronized (lock) {
            return evictions;
        }
    }
}
