package org.lts.qualityessentials.books;

import net.minecraft.client.gui.components.MultilineTextField;

public interface RichPage {
    void qualityessentials$setRich(boolean rich, boolean marks);

    boolean qualityessentials$rich();

    MultilineTextField qualityessentials$field();
}
