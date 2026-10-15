package org.lts.qualityessentials.mixin;

import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BookEditScreen.class)
public interface BookEditScreenAccessor {
    @Accessor("pages")
    List<String> qualityessentials$pages();

    @Accessor("currentPage")
    int qualityessentials$currentPage();

    @Accessor("currentPage")
    void qualityessentials$setCurrentPage(int page);

    @Accessor("page")
    MultiLineEditBox qualityessentials$page();

    @Accessor("book")
    ItemStack qualityessentials$book();

    @Invoker("updatePageContent")
    void qualityessentials$updatePageContent();

    @Invoker("updateButtonVisibility")
    void qualityessentials$updateButtonVisibility();
}
