package org.lts.qualityessentials.shulker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

public final class ShulkerContents {
    public static final int COLUMNS = 9;

    private ShulkerContents() {
    }

    public static int gridSize(int declared, int lastFilled, boolean shulkerBox) {
        if (shulkerBox) {
            return 27;
        }
        int needed = Math.max(declared, lastFilled + 1);
        return Math.max(COLUMNS, (needed + COLUMNS - 1) / COLUMNS * COLUMNS);
    }

    public static <T> List<Group<T>> merge(List<T> items, ToIntFunction<T> count, BiPredicate<T, T> same) {
        List<Group<T>> groups = new ArrayList<>();
        for (T item : items) {
            Group<T> match = null;
            for (Group<T> group : groups) {
                if (same.test(group.item, item)) {
                    match = group;
                    break;
                }
            }
            if (match == null) {
                groups.add(new Group<>(item, count.applyAsInt(item)));
            } else {
                match.count += count.applyAsInt(item);
            }
        }
        groups.sort((a, b) -> Integer.compare(b.count, a.count));
        return groups;
    }

    public static String countLabel(int count) {
        if (count < 1000) {
            return Integer.toString(count);
        }
        int tenths = count / 100;
        return tenths % 10 == 0 ? tenths / 10 + "k" : tenths / 10 + "." + tenths % 10 + "k";
    }

    public static final class Group<T> {
        public final T item;
        public int count;

        Group(T item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
