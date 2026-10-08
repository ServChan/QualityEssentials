package org.lts.qualityessentials.keyboard;

import net.minecraft.client.KeyMapping;

public interface ModifierSource {
    boolean handles(KeyMapping mapping);

    int modifiers(KeyMapping mapping);

    void setModifiers(KeyMapping mapping, int mask);
}
