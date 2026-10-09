package org.lts.qualityessentials.newitems;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class FreshSlots<K> {
    private final Object[] keys;
    private final int[] counts;
    private final Object[] fresh;

    public FreshSlots(int size) {
        keys = new Object[size];
        counts = new int[size];
        fresh = new Object[size];
    }

    public int size() {
        return keys.length;
    }

    public void snapshot(K[] currentKeys, int[] currentCounts) {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = currentKeys[i];
            counts[i] = currentCounts[i];
        }
        dropStale(currentKeys);
    }

    public void update(K[] currentKeys, int[] currentCounts, boolean canMark) {
        if (canMark) {
            Map<Object, Integer> before = totals(keys, counts);
            Map<Object, Integer> after = totals(currentKeys, currentCounts);
            for (int i = 0; i < keys.length; i++) {
                K key = currentKeys[i];
                if (key == null) {
                    continue;
                }
                boolean grew = !Objects.equals(keys[i], key) || currentCounts[i] > counts[i];
                if (grew && after.getOrDefault(key, 0) > before.getOrDefault(key, 0)) {
                    fresh[i] = key;
                }
            }
        }
        snapshot(currentKeys, currentCounts);
    }

    public boolean isFresh(int slot) {
        return slot >= 0 && slot < fresh.length && fresh[slot] != null;
    }

    public void seen(int slot) {
        if (slot >= 0 && slot < fresh.length) {
            fresh[slot] = null;
        }
    }

    public void clear() {
        for (int i = 0; i < keys.length; i++) {
            keys[i] = null;
            counts[i] = 0;
            fresh[i] = null;
        }
    }

    public int freshCount() {
        int total = 0;
        for (Object key : fresh) {
            if (key != null) {
                total++;
            }
        }
        return total;
    }

    private void dropStale(K[] currentKeys) {
        for (int i = 0; i < fresh.length; i++) {
            if (fresh[i] != null && !Objects.equals(fresh[i], currentKeys[i])) {
                fresh[i] = null;
            }
        }
    }

    private static Map<Object, Integer> totals(Object[] keys, int[] counts) {
        Map<Object, Integer> totals = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null) {
                totals.merge(keys[i], counts[i], Integer::sum);
            }
        }
        return totals;
    }
}
