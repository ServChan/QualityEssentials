package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(BookViewScreen.class)
public interface BookViewScreenAccessor {
    @Accessor("bookAccess")
    BookViewScreen.BookAccess qualityessentials$bookAccess();

    @Accessor("currentPage")
    int qualityessentials$currentPage();

    @Invoker("pageBack")
    void qualityessentials$pageBack();

    @Invoker("pageForward")
    void qualityessentials$pageForward();

    @Invoker("forcePage")
    boolean qualityessentials$forcePage(int page);
}
