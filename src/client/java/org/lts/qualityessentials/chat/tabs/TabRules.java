package org.lts.qualityessentials.chat.tabs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class TabRules {
    public static final String EVERYTHING = "*";
    private static final int MAX_GROUPS = 63;

    private final List<String> groupIds = new ArrayList<>();
    private final List<List<Pattern>> patterns = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private final List<long[]> masks = new ArrayList<>();

    public TabRules(List<Group> groups, List<Tab> tabs, Consumer<String> problem) {
        for (Group group : groups) {
            if (group == null || group.id() == null || group.id().isBlank() || groupIds.contains(group.id())) {
                continue;
            }
            if (groupIds.size() >= MAX_GROUPS) {
                problem.accept("too many groups, ignoring " + group.id());
                continue;
            }
            List<Pattern> compiled = new ArrayList<>();
            for (String pattern : group.patterns() == null ? List.<String>of() : group.patterns()) {
                if (pattern == null || pattern.isBlank()) {
                    continue;
                }
                try {
                    compiled.add(Pattern.compile(pattern));
                } catch (PatternSyntaxException exception) {
                    problem.accept(group.id() + ": " + exception.getDescription() + " in " + pattern);
                }
            }
            groupIds.add(group.id());
            patterns.add(compiled);
        }
        for (Tab tab : tabs) {
            if (tab == null || tab.id() == null || tab.id().isBlank()) {
                continue;
            }
            List<String> show = tab.show() == null || tab.show().isEmpty() ? List.of(EVERYTHING) : tab.show();
            List<String> hide = tab.hide() == null ? List.of() : tab.hide();
            this.tabs.add(new Tab(tab.id(), tab.name(), show, hide));
            masks.add(new long[] {show.contains(EVERYTHING) ? -1L : mask(show, problem, tab.id()), mask(hide, problem, tab.id())});
        }
        if (this.tabs.isEmpty()) {
            this.tabs.add(new Tab("all", null, List.of(EVERYTHING), List.of()));
            masks.add(new long[] {-1L, 0L});
        }
    }

    private long mask(List<String> ids, Consumer<String> problem, String tab) {
        long mask = 0L;
        for (String id : ids) {
            int index = groupIds.indexOf(id);
            if (index >= 0) {
                mask |= 1L << index;
            } else if (!EVERYTHING.equals(id)) {
                problem.accept(tab + ": unknown group " + id);
            }
        }
        return mask;
    }

    public long classify(String text) {
        long mask = 0L;
        for (int i = 0; i < patterns.size(); i++) {
            for (Pattern pattern : patterns.get(i)) {
                if (pattern.matcher(text).find()) {
                    mask |= 1L << i;
                    break;
                }
            }
        }
        return mask;
    }

    public boolean visible(int tab, long mask) {
        long[] rule = masks.get(tab);
        return (rule[0] == -1L || (mask & rule[0]) != 0) && (mask & rule[1]) == 0;
    }

    public boolean showsEverything(int tab) {
        long[] rule = masks.get(tab);
        return rule[0] == -1L && rule[1] == 0;
    }

    public boolean alwaysPresent(int tab) {
        return masks.get(tab)[0] == -1L;
    }

    public int size() {
        return tabs.size();
    }

    public Tab tab(int index) {
        return tabs.get(index);
    }

    public int indexOf(String id) {
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).id().equals(id)) {
                return i;
            }
        }
        return 0;
    }

    public record Group(String id, List<String> patterns) {
    }

    public record Tab(String id, String name, List<String> show, List<String> hide) {
    }
}
