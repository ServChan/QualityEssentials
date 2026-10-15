package org.lts.qualityessentials.gui;

public interface ScreenState {
    Object qualityessentials$state();

    void qualityessentials$setState(Object state);

    @SuppressWarnings("unchecked")
    static <T> T get(Object screen, Class<T> type) {
        Object state = ((ScreenState) screen).qualityessentials$state();
        return type.isInstance(state) ? (T) state : null;
    }

    static void set(Object screen, Object state) {
        ((ScreenState) screen).qualityessentials$setState(state);
    }
}
