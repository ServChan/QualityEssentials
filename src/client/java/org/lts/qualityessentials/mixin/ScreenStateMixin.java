package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.screens.Screen;
import org.lts.qualityessentials.gui.ScreenState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Screen.class)
public abstract class ScreenStateMixin implements ScreenState {
    @Unique
    private Object qualityessentials$state;

    @Override
    public Object qualityessentials$state() {
        return qualityessentials$state;
    }

    @Override
    public void qualityessentials$setState(Object state) {
        this.qualityessentials$state = state;
    }
}
