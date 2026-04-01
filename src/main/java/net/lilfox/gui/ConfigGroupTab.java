package net.lilfox.gui;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.Component;
import net.lilfox.config.ConfigGroup;

import java.util.function.Consumer;

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
     */
    public ConfigGroupTab(ConfigGroup group, LilConfigScreen screen) {
        this.title = Component.translatable(group.getName());
        this.list  = new ConfigEntryList(screen, group);
    }

    /** Returns the list widget so the screen can add it to the render loop. */
    public ConfigEntryList getList() {
        return list;
    }

    @Override
    public Component getTabTitle() {
        return title;
    }

    @Override
    public Component getTabExtraNarration() {
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
