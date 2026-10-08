package org.lts.qualityessentials.keyboard;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lts.qualityessentials.keyboard.model.Binding;
import org.lts.qualityessentials.keyboard.model.ConflictAnalyzer;

import java.util.ArrayList;
import java.util.List;

public record BindingSnapshot(List<KeyMapping> mappings, List<Binding> bindings, ConflictAnalyzer.Result conflicts) {

    public static BindingSnapshot capture(Minecraft minecraft) {
        List<KeyMapping> mappings = new ArrayList<>();
        List<Binding> bindings = new ArrayList<>();
        for (KeyMapping mapping : minecraft.options.keyMappings) {
            if (mapping.getCategory() == KeyMapping.Category.DEBUG) {
                continue;
            }
            mappings.add(mapping);
            bindings.add(toBinding(mapping));
        }
        return new BindingSnapshot(List.copyOf(mappings), List.copyOf(bindings), ConflictAnalyzer.analyze(bindings));
    }

    static Binding toBinding(KeyMapping mapping) {
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        ModifierSource source = ModifierRegistry.sourceFor(mapping);
        int modifiers = source == null ? 0 : source.modifiers(mapping);
        return new Binding(mapping.getName(), mapping.getCategory().id().toString(),
                key.getType() == InputConstants.Type.MOUSE, key.getValue(), modifiers, mapping.isUnbound());
    }

    public List<Integer> indicesOn(boolean mouse, int code, int layer) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < bindings.size(); i++) {
            Binding binding = bindings.get(i);
            if (binding.onKey(mouse, code) && binding.visibleOnLayer(layer)) {
                result.add(i);
            }
        }
        return result;
    }

    public ConflictAnalyzer.Level worstOn(boolean mouse, int code, int layer) {
        ConflictAnalyzer.Level worst = ConflictAnalyzer.Level.NONE;
        for (int index : indicesOn(mouse, code, layer)) {
            ConflictAnalyzer.Level level = conflicts.levelOf(index);
            if (level.ordinal() > worst.ordinal()) {
                worst = level;
            }
        }
        return worst;
    }

    public List<Integer> conflicting() {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < bindings.size(); i++) {
            if (conflicts.levelOf(i) != ConflictAnalyzer.Level.NONE) {
                result.add(i);
            }
        }
        return result;
    }
}
