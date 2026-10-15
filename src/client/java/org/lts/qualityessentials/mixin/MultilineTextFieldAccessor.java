package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultilineTextField.class)
public interface MultilineTextFieldAccessor {
    @Accessor("cursor")
    void qualityessentials$setCursor(int cursor);

    @Accessor("selectCursor")
    int qualityessentials$selectCursor();

    @Accessor("selectCursor")
    void qualityessentials$setSelectCursor(int selectCursor);

    @Accessor("width")
    int qualityessentials$width();
}
