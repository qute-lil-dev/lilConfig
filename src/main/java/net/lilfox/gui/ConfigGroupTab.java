package net.lilfox.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.lilfox.config.ConfigGroup;
import net.lilfox.util.I18nHelper;

import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

/**
 * A {@link Tab} that wraps a {@link ConfigGroup} and owns the
 * {@link ConfigEntryList} shown when this tab is selected.
 */
public class ConfigGroupTab implements Tab {

    private final Component title;
    private final ConfigEntryList list;

    /**
     * Creates a tab backed by the given group; the list is pre-populated.
     *
     * @param group   the config group to display
     * @param screen  the owning screen (needed to size the list)
     * @param modId   the owning mod id, used to resolve the tab i18n key
     */
    public ConfigGroupTab(ConfigGroup group, LilConfigScreen screen, String modId) {
        this.title = I18nHelper.tabLabel(modId, group.getName());
        this.list  = new ConfigEntryList(screen, group);
    }

    /** Returns the list widget so the screen can add it to the render loop. */
    public ConfigEntryList getList() {
        return list;
    }

    @Override
    public @NonNull Component getTabTitle() {
        return title;
    }

    @Override
    public @NonNull Component getTabExtraNarration() {
        return Component.empty();
    }

    @Override
    public void visitChildren(Consumer<AbstractWidget> visitor) {
        visitor.accept(list);
    }

    @Override
    public void doLayout(ScreenRectangle area) {
        list.updateSizeAndPosition(area.width(), area.height(), area.top());
        list.setX(area.left());
    }
}
