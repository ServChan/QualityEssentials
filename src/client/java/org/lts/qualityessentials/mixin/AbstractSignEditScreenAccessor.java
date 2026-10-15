package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractSignEditScreen.class)
public interface AbstractSignEditScreenAccessor {
    @Accessor("messages")
    String[] qualityessentials$messages();

    @Accessor("line")
    int qualityessentials$line();

    @Accessor("line")
    void qualityessentials$setLine(int line);

    @Accessor("signField")
    TextFieldHelper qualityessentials$signField();

    @Accessor("sign")
    SignBlockEntity qualityessentials$sign();

    @Invoker("setMessage")
    void qualityessentials$setMessage(String message);

    @Invoker("getSignYOffset")
    float qualityessentials$signYOffset();

    @Invoker("getSignTextScale")
    Vector3fc qualityessentials$signTextScale();
}
