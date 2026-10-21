package org.lts.qualityessentials.shulker;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShulkerMemory<T> {
    public static final int MAX_ENTRIES = 4096;
    private static final long MINUTE = 60_000L;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    private final Map<String, Entry<T>> entries = new LinkedHashMap<>();
    private final int limit;

    public ShulkerMemory() {
        this(MAX_ENTRIES);
    }

    ShulkerMemory(int limit) {
        this.limit = limit;
    }

    public Entry<T> get(String dimension, long pos) {
        return entries.get(key(dimension, pos));
    }

    public void put(Entry<T> entry) {
        String key = key(entry.dimension(), entry.pos());
        Entry<T> existing = entries.get(key);
        if (existing != null && existing.seenAt() > entry.seenAt()) {
            return;
        }
        entries.remove(key);
        entries.put(key, entry);
        if (entries.size() > limit) {
            entries.values().stream().min(Comparator.comparingLong(Entry::seenAt))
                    .ifPresent(oldest -> entries.remove(key(oldest.dimension(), oldest.pos())));
        }
    }

    public boolean remove(String dimension, long pos) {
        return entries.remove(key(dimension, pos)) != null;
    }

    public List<Entry<T>> entries() {
        return new ArrayList<>(entries.values());
    }

    public int size() {
        return entries.size();
    }

    public static Age age(long seenAt, long now) {
        long elapsed = Math.max(0L, now - seenAt);
        if (elapsed < MINUTE) {
            return new Age(Age.Unit.NOW, 0);
        }
        if (elapsed < HOUR) {
            return new Age(Age.Unit.MINUTES, elapsed / MINUTE);
        }
        if (elapsed < 2 * DAY) {
            return new Age(Age.Unit.HOURS, elapsed / HOUR);
        }
        return new Age(Age.Unit.DAYS, elapsed / DAY);
    }

    private static String key(String dimension, long pos) {
        return dimension + "|" + pos;
    }

    public record Entry<T>(String dimension, long pos, String block, T contents, long seenAt) {
    }

    public record Age(Unit unit, long value) {
        public enum Unit {
            NOW, MINUTES, HOURS, DAYS
        }
    }
}
