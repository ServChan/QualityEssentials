package org.lts.qualityessentials.keyboard.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ConflictAnalyzer {
    private static final Set<String> HOLD_ACTIVATORS = Set.of("key.saveToolbarActivator", "key.loadToolbarActivator",
            "key.spectatorHotbar", "key.spectatorOutlines");

    private ConflictAnalyzer() {
    }

    public static Level between(Binding a, Binding b) {
        if (a == b || !a.sameKey(b)) {
            return Level.NONE;
        }
        Level level;
        if (a.modifiers() == b.modifiers()) {
            level = Level.HARD;
        } else if ((a.modifiers() & b.modifiers()) == a.modifiers() || (a.modifiers() & b.modifiers()) == b.modifiers()) {
            level = Level.SOFT;
        } else {
            return Level.NONE;
        }
        if (level == Level.HARD && (HOLD_ACTIVATORS.contains(a.name()) || HOLD_ACTIVATORS.contains(b.name()))) {
            return Level.SOFT;
        }
        return level;
    }

    public static Result analyze(List<Binding> bindings) {
        Level[] levels = new Level[bindings.size()];
        java.util.Arrays.fill(levels, Level.NONE);
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < bindings.size(); i++) {
            for (int j = i + 1; j < bindings.size(); j++) {
                Level level = between(bindings.get(i), bindings.get(j));
                if (level == Level.NONE) {
                    continue;
                }
                pairs.add(new int[]{i, j, level.ordinal()});
                levels[i] = Level.worst(levels[i], level);
                levels[j] = Level.worst(levels[j], level);
            }
        }
        return new Result(List.of(levels), List.copyOf(pairs));
    }

    public enum Level {
        NONE,
        SOFT,
        HARD;

        static Level worst(Level a, Level b) {
            return a.ordinal() >= b.ordinal() ? a : b;
        }
    }

    public record Result(List<Level> levels, List<int[]> pairs) {
        public Level levelOf(int index) {
            return levels.get(index);
        }

        public long hardCount() {
            return pairs.stream().filter(pair -> pair[2] == Level.HARD.ordinal()).count();
        }

        public long softCount() {
            return pairs.stream().filter(pair -> pair[2] == Level.SOFT.ordinal()).count();
        }

        public List<Integer> partnersOf(int index) {
            List<Integer> partners = new ArrayList<>();
            for (int[] pair : pairs) {
                if (pair[0] == index) {
                    partners.add(pair[1]);
                } else if (pair[1] == index) {
                    partners.add(pair[0]);
                }
            }
            return partners;
        }
    }
}
