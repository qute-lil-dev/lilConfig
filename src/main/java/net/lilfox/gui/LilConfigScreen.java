package net.lilfox.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.lilfox.config.IConfig;
import net.lilfox.config.IConfigHotkey;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.IConfigProvider;
import net.lilfox.manager.LilConfigManager;

import org.jspecify.annotations.NonNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The main config GUI screen for a single {@link IConfigProvider}.
 *
 * <p>Layout:
 * <pre>
 *   [ TabNavigationBar  ]   ← TAB_BAR_H px
 *   [  ConfigEntryList  ]   ← fills remaining height minus footer
 *   [    Done button    ]   ← FOOTER_H px
 * </pre>
 */
public class LilConfigScreen extends Screen {

    /** Height of the title area above the tab navigation bar. */
    static final int TITLE_H   = 20;
    /** Height of the tab navigation bar. */
    static final int TAB_BAR_H = 24;
    /** Height of the footer area containing the Done button. */
    static final int FOOTER_H  = 36;

    private final Screen parent;
    private final IConfigProvider provider;

    private TabNavigationBar tabBar;
    private TabManager       tabManager;
    private final List<ConfigGroupTab> tabs = new ArrayList<>();
    private Button           doneButton;

    /** Non-null while the user is rebinding a key: the widget being updated. */
    private IConfigHotkey rebindTarget;
    private Button        rebindButton;
    /** Accumulates key presses during an active rebind session. */
    private KeyBind       pendingBind = KeyBind.NONE;
    /** Snapshot of the bind that was active before the current rebind session. */
    private KeyBind       priorBind   = KeyBind.NONE;

    /**
     * Opens this screen for the given provider, pushing it over the current screen.
     *
     * @param provider the config provider to display
     */
    public static void open(IConfigProvider provider) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new LilConfigScreen(mc.screen, provider));
    }

    /**
     * Creates a new screen instance for the given provider and parent screen.
     * Intended for use by ModMenu, which manages the screen transition itself.
     *
     * @param parent   the screen to return to when this screen is closed
     * @param provider the config provider to display
     * @return a new {@code LilConfigScreen} instance
     */
    public static LilConfigScreen create(Screen parent, IConfigProvider provider) {
        return new LilConfigScreen(parent, provider);
    }

    private LilConfigScreen(Screen parent, IConfigProvider provider) {
        super(Component.literal(provider.getDisplayName()));
        this.parent   = parent;
        this.provider = provider;
    }

    // -------------------------------------------------------------------------
    // Screen lifecycle
    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        tabs.clear();

        tabManager = new TabManager(this::addRenderableWidget, this::removeWidget);

        List<ConfigGroupTab> groupTabs = provider.getConfigGroups().stream()
                .map(g -> new ConfigGroupTab(g, this))
                .toList();
        tabs.addAll(groupTabs);

        TabNavigationBar.Builder builder = TabNavigationBar.builder(tabManager, width);
        groupTabs.forEach(builder::addTabs);
        tabBar = builder.build();
        addRenderableWidget(tabBar);

        // Done button
        doneButton = Button.builder(CommonComponents.GUI_DONE, btn -> onClose())
                .bounds(width / 2 - 100, height - FOOTER_H + 6, 200, 20)
                .build();
        addRenderableWidget(doneButton);

        // Set the content area so doLayout is called when selectTab is invoked
        tabManager.setTabArea(contentArea());

        if (!groupTabs.isEmpty()) {
            tabBar.selectTab(0, false);
        }
        tabBar.arrangeElements();
        setTabBarY(tabBar, TITLE_H);
    }

    @Override
    public void repositionElements() {
        if (tabBar == null) return;
        tabBar.updateWidth(width);
        tabBar.arrangeElements();
        setTabBarY(tabBar, TITLE_H);
        tabManager.setTabArea(contentArea());
        if (doneButton != null) {
            doneButton.setX(width / 2 - 100);
            doneButton.setY(height - FOOTER_H + 6);
        }
    }

    private ScreenRectangle contentArea() {
        return new ScreenRectangle(0, TITLE_H + TAB_BAR_H, width, height - TITLE_H - TAB_BAR_H - FOOTER_H);
    }

    /**
     * Repositions the tab bar's internal layout to the given Y coordinate.
     * {@code TabNavigationBar.arrangeElements()} hardcodes Y=0, so reflection
     * is used to reach the private {@code layout} field.
     */
    private static void setTabBarY(TabNavigationBar bar, int y) {
        try {
            java.lang.reflect.Field f = TabNavigationBar.class.getDeclaredField("layout");
            f.setAccessible(true);
            ((net.minecraft.client.gui.layouts.LinearLayout) f.get(bar)).setY(y);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @Override
    public void onClose() {
        commitRebind();
        LilConfigManager.getInstance().saveAll();
        minecraft.setScreen(parent);
    }

    /**
     * Returns {@code true} if {@code config} is the entry currently being rebound.
     * Used by {@link ConfigEntryList.ConfigRow} to skip per-frame label updates
     * while the rebind placeholder is displayed.
     *
     * @param config the config entry to test
     * @return {@code true} if an active rebind session targets this entry
     */
    boolean isRebinding(IConfig config) {
        return config instanceof IConfigHotkey hk && rebindTarget == hk;
    }

    /**
     * Confirms the in-progress rebind session using whatever combo is currently
     * accumulated. If nothing was accumulated, restores the prior binding.
     * Does nothing if no session is active.
     */
    private void commitRebind() {
        if (rebindTarget == null) return;
        finishRebind(pendingBind.getKeys().isEmpty() ? priorBind : pendingBind);
    }

    /**
     * Saves {@code result} to the rebind target and resets all rebind state.
     *
     * @param result the KeyBind to assign; may be {@link KeyBind#NONE}
     */
    private void finishRebind(KeyBind result) {
        rebindTarget.setKeyBind(result);
        rebindButton.setMessage(ConfigEntryList.keyLabel(result, rebindTarget));
        rebindTarget = null;
        rebindButton = null;
        pendingBind  = KeyBind.NONE;
        priorBind    = KeyBind.NONE;
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        extractMenuBackground(gfx);
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        // Draw title in the area above the tab bar
        gfx.centeredText(font, title, width / 2, TITLE_H / 2 - font.lineHeight / 2, -1);
    }

    // -------------------------------------------------------------------------
    // Hotkey rebind
    // -------------------------------------------------------------------------

    /**
     * Called by a row widget when the user clicks a hotkey button to start rebinding.
     * Press additional keys to build a combo, ENTER to confirm, ESC to cancel.
     *
     * @param target the config entry that owns the binding
     * @param btn    the button widget to update with the new label
     */
    public void startRebind(IConfigHotkey target, Button btn) {
        commitRebind();
        this.rebindTarget = target;
        this.rebindButton = btn;
        this.priorBind    = target.getKeyBind();
        this.pendingBind  = KeyBind.NONE;
        btn.setMessage(Component.literal("> ... <"));
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent event) {
        if (rebindTarget != null) {
            int key = event.key();
            if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                // First press → clear to NONE; subsequent press → confirm accumulated combo.
                finishRebind(pendingBind.getKeys().isEmpty() ? KeyBind.NONE : pendingBind);
            } else {
                pendingBind = pendingBind.withKey(InputConstants.Type.KEYSYM.getOrCreate(key));
                rebindButton.setMessage(Component.literal(pendingBind.toDisplayString() + " ..."));
            }
            return true;
        }
        return super.keyPressed(event);
    }

    /**
     * During an active rebind session:
     * <ul>
     *   <li>Click on the rebind button → add the mouse button to the combo.</li>
     *   <li>Click anywhere else → confirm and exit rebind.</li>
     * </ul>
     * The initial click that opens the rebind session is never intercepted here
     * because {@link #rebindTarget} is still {@code null} at that point.
     */
    @Override
    public boolean mouseClicked(@NonNull MouseButtonEvent event, boolean doubleClick) {
        if (rebindTarget != null) {
            if (rebindButton.isMouseOver(event.x(), event.y())) {
                InputConstants.Key mouseKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
                pendingBind = pendingBind.withKey(mouseKey);
                rebindButton.setMessage(
                        Component.literal(pendingBind.toDisplayString() + " ..."));
            } else {
                commitRebind();
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
