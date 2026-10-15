package org.lts.qualityessentials.books;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.gui.screens.inventory.BookSignScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.world.item.component.WrittenBookContent;
import org.lts.qualityessentials.core.Module;
import org.lts.qualityessentials.core.QE;
import org.lts.qualityessentials.mixin.BookSignScreenAccessor;

public final class BookModule {
    private BookModule() {
    }

    public static void init() {
        ScreenEvents.AFTER_INIT.register((minecraft, screen, width, height) -> {
            if (!QE.active(Module.BOOKS)) {
                return;
            }
            if (screen instanceof BookEditScreen edit) {
                BookEditor.attach(edit, minecraft, width, height);
            } else if (screen instanceof BookViewScreen view) {
                BookReader.attach(view, width);
            } else if (screen instanceof BookSignScreen sign && QE.config().booksLongTitle) {
                ((BookSignScreenAccessor) sign).qualityessentials$titleBox().setMaxLength(WrittenBookContent.TITLE_MAX_LENGTH);
            }
        });
    }
}
