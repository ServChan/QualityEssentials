package org.lts.qualityessentials.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiPredicate;
import java.util.function.LongSupplier;

public final class EditHistory<T> {
    private final int limit;
    private final long mergeMillis;
    private final LongSupplier clock;
    private final BiPredicate<T, T> same;
    private final Deque<T> undo = new ArrayDeque<>();
    private final Deque<T> redo = new ArrayDeque<>();
    private T current;
    private boolean typing;
    private long lastTyping;

    public EditHistory(int limit, long mergeMillis, LongSupplier clock, BiPredicate<T, T> same) {
        this.limit = limit;
        this.mergeMillis = mergeMillis;
        this.clock = clock;
        this.same = same;
    }

    public void observe(T state, boolean typed) {
        if (current == null) {
            current = state;
            return;
        }
        if (same.test(current, state)) {
            current = state;
            return;
        }
        long now = clock.getAsLong();
        boolean merge = typed && typing && now - lastTyping < mergeMillis;
        if (!merge) {
            undo.push(current);
            while (undo.size() > limit) {
                undo.removeLast();
            }
        }
        redo.clear();
        current = state;
        typing = typed;
        lastTyping = now;
    }

    public T undo(T live) {
        observe(live, false);
        if (undo.isEmpty()) {
            return null;
        }
        redo.push(current);
        current = undo.pop();
        typing = false;
        return current;
    }

    public T redo(T live) {
        observe(live, false);
        if (redo.isEmpty()) {
            return null;
        }
        undo.push(current);
        current = redo.pop();
        typing = false;
        return current;
    }

    public boolean canUndo(T live) {
        return !undo.isEmpty() || current != null && !same.test(current, live);
    }

    public boolean canRedo(T live) {
        return !redo.isEmpty() && (current == null || same.test(current, live));
    }
}
