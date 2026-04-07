package net.lilfox.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.lilfox.config.*;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.LilConfigManager;
import net.lilfox.util.I18nHelper;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * Scrollable list that renders one row per {@link IConfig} entry.
 *
 * <p>Row layout (all widths in px):
 * <pre>
 *   [name label]  [     WIDGET_ZONE     ] [Effect?] [Reset?]
 *                 |-- HOTKEY_BTN_W -|-2-|-- TOGGLE_BTN_W --|
 * </pre>
 * {@code ConfigBoolean} fills the entire zone with its toggle button so it
 * aligns with the two-button layout of {@code ConfigHotkeyedBoolean}.
 */
public class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryList.ConfigRow> {

    // ----- layout constants -----
    static final int HOTKEY_BTN_W  = 100;
    static final int TOGGLE_BTN_W  = 60;
    static final int ROW_PADDING   = 4;
    /** Total widget zone = hotkey btn + gap + toggle btn; matches two-button layout width exactly. */
    static final int WIDGET_ZONE   = HOTKEY_BTN_W + ROW_PADDING + TOGGLE_BTN_W;
    static final int EFFECT_BTN_W  = 60;
    static final int RESET_BTN_W   = 50;
    static final int BTN_H         = 20;

    private final LilConfigScreen owner;

    public ConfigEntryList(LilConfigScreen screen, ConfigGroup group) {
        super(
            Minecraft.getInstance(),
            screen.width,
            screen.height - LilConfigScreen.TAB_BAR_H - LilConfigScreen.FOOTER_H,
            LilConfigScreen.TAB_BAR_H,
            BTN_H + 8
        );
        this.owner = screen;
        this.centerListVertically = false;
        for (IConfig cfg : group.getConfigs()) {
            if (cfg.getType() == ConfigType.SEPARATOR) {
                addEntry(new SeparatorRow(cfg, screen));
            } else {
                addEntry(new ConfigRow(cfg, screen));
            }
        }
    }

    @Override
    public int getRowWidth() {
        return Math.min(owner.width - 50, 400);
    }

    /**
     * Returns a styled component for a hotkey button label.
     * Shows {@code "---"} for empty/NONE bindings.
     * Applies {@link ChatFormatting#GOLD} when the binding conflicts with another
     * registered hotkey (subset/superset overlap).
     */
    static Component keyLabel(KeyBind kb, IConfigHotkey owner) {
        String s = kb.getKeys().isEmpty() ? "---" : kb.toDisplayString();
        if (LilConfigManager.getInstance().isConflicting(kb, owner)) {
            return Component.literal(s).withStyle(ChatFormatting.GOLD);
        }
        return Component.literal(s);
    }

    // -------------------------------------------------------------------------

    /**
     * A single row inside the list, containing the widgets for one {@link IConfig}.
     */
    public static class ConfigRow extends ContainerObjectSelectionList.Entry<ConfigRow> {

        protected final IConfig config;
        private final LilConfigScreen screen;
        private final List<AbstractWidget> widgets = new ArrayList<>();
        private Button resetButton;
        private Button hotkeyButton;

        public ConfigRow(IConfig config, LilConfigScreen screen) {
            this.config = config;
            this.screen = screen;
            buildWidgets(screen);
        }

        private void buildWidgets(LilConfigScreen screen) {
            // placeholder x/y; repositionWidgets() positions them each frame
            switch (config.getType()) {
                case BOOLEAN -> {
                    IConfigBoolean b = (IConfigBoolean) config;
                    Button.Builder tb = Button.builder(boolLabel(b.getValue()),
                            btn -> { b.setValue(!b.getValue()); btn.setMessage(boolLabel(b.getValue())); })
                            .size(WIDGET_ZONE, BTN_H).pos(0, 0);
                    Component descTip = I18nHelper.desc(config);
                    if (descTip != null) tb.tooltip(Tooltip.create(descTip));
                    widgets.add(tb.build());
                }
                case HOTKEYED_BOOLEAN -> {
                    IConfigHotkey  hk = (IConfigHotkey)  config;
                    IConfigBoolean b  = (IConfigBoolean) config;
                    hotkeyButton = Button.builder(keyLabel(hk.getKeyBind(), hk),
                            btn -> screen.startRebind(hk, btn))
                            .tooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.rebind")))
                            .size(HOTKEY_BTN_W, BTN_H).pos(0, 0).build();
                    Button toggleBtn = Button.builder(boolLabel(b.getValue()),
                            btn -> { b.setValue(!b.getValue()); btn.setMessage(boolLabel(b.getValue())); })
                            .size(TOGGLE_BTN_W, BTN_H).pos(0, 0).build();
                    widgets.add(hotkeyButton);
                    widgets.add(toggleBtn);
                }
                case HOTKEY -> {
                    IConfigHotkey hk = (IConfigHotkey) config;
                    hotkeyButton = Button.builder(keyLabel(hk.getKeyBind(), hk),
                            btn -> screen.startRebind(hk, btn))
                            .tooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.rebind")))
                            .size(WIDGET_ZONE, BTN_H).pos(0, 0).build();
                    widgets.add(hotkeyButton);
                }
                case INTEGER -> {
                    ConfigInteger ci = (ConfigInteger) config;
                    EditBox box = new EditBox(Minecraft.getInstance().font,
                            0, 0, WIDGET_ZONE, BTN_H,
                            Component.literal(ci.getName()));
                    box.setValue(String.valueOf(ci.getValue()));
                    box.setResponder(s -> {
                        try { ci.setValue(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
                    });
                    box.setTooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.range",
                            ci.getMinValue(), ci.getMaxValue())));
                    widgets.add(box);
                }
                case STRING -> {
                    ConfigString cs = (ConfigString) config;
                    EditBox box = new EditBox(Minecraft.getInstance().font,
                            0, 0, WIDGET_ZONE, BTN_H,
                            I18nHelper.label(config));
                    box.setValue(cs.getValue());
                    box.setResponder(cs::setValue);
                    Component descTip = I18nHelper.desc(config);
                    if (descTip != null) box.setTooltip(Tooltip.create(descTip));
                    widgets.add(box);
                }
                case OPTION_LIST -> {
                    IConfigOptionList<?> cfg = (IConfigOptionList<?>) config;
                    Button.Builder ob = Button.builder(
                                    optionLabel(cfg),
                                    b -> { cfg.cycle(); b.setMessage(optionLabel(cfg)); })
                            .size(WIDGET_ZONE, BTN_H).pos(0, 0);
                    Component descTip = I18nHelper.desc(config);
                    if (descTip != null) ob.tooltip(Tooltip.create(descTip));
                    widgets.add(ob.build());
                }
                case DOUBLE -> {
                    ConfigDouble cd = (ConfigDouble) config;
                    EditBox box = new EditBox(Minecraft.getInstance().font,
                            0, 0, WIDGET_ZONE, BTN_H,
                            Component.literal(cd.getName()));
                    box.setValue(String.valueOf(cd.getValue()));
                    box.setResponder(s -> {
                        try { cd.setValue(Double.parseDouble(s.trim())); } catch (NumberFormatException ignored) {}
                    });
                    if (Double.isFinite(cd.getMin()) || Double.isFinite(cd.getMax())) {
                        box.setTooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.range",
                                cd.getMin(), cd.getMax())));
                    }
                    widgets.add(box);
                }
                case SEPARATOR -> { return; }
            }

            // optional effect button
            if (config.getEffectButtonLabel() != null) {
                Runnable action = config.getEffectAction();
                widgets.add(Button.builder(Component.translatable(config.getEffectButtonLabel()),
                        btn -> { if (action != null) action.run(); })
                        .size(EFFECT_BTN_W, BTN_H).pos(0, 0).build());
            }

            // reset button
            Button resetBtn = Button.builder(Component.translatable("lilconfig.reset"), btn -> resetRow())
                    .tooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.reset")))
                    .size(RESET_BTN_W, BTN_H).pos(0, 0).build();
            widgets.add(resetButton = resetBtn);
        }

        /** Repositions all widgets based on the entry's current content bounds. */
        private void repositionWidgets() {
            int rowRight = getContentRight();
            int cy       = getContentYMiddle() - BTN_H / 2;

            int x = rowRight;
            for (int i = widgets.size() - 1; i >= 0; i--) {
                AbstractWidget w = widgets.get(i);
                x -= w.getWidth();
                w.setX(x);
                w.setY(cy);
                x -= ROW_PADDING;
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            repositionWidgets();
            if (resetButton != null) resetButton.active = config.isModified();
            if (hotkeyButton != null && !screen.isRebinding(config)) {
                IConfigHotkey hk = (IConfigHotkey) config;
                hotkeyButton.setMessage(keyLabel(hk.getKeyBind(), hk));
            }
            // Draw config name using the entry's own content coordinates, not the mouse position
            int textY = getContentY() + (getContentHeight() - Minecraft.getInstance().font.lineHeight) / 2;
            Component label = I18nHelper.label(config);
            gfx.text(Minecraft.getInstance().font, label, getContentX(), textY, -1);
            int labelW = Minecraft.getInstance().font.width(label);
            if (mouseX >= getContentX() && mouseX < getContentX() + labelW
                    && mouseY >= textY && mouseY < textY + Minecraft.getInstance().font.lineHeight) {
                Component desc = I18nHelper.desc(config);
                if (desc != null) {
                    gfx.setTooltipForNextFrame(desc, mouseX, mouseY);
                }
            }
            for (AbstractWidget w : widgets) {
                w.extractRenderState(gfx, mouseX, mouseY, partialTick);
            }
        }

        @Override
        public @NonNull List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
            return widgets;
        }

        @Override
        public @NonNull List<? extends net.minecraft.client.gui.narration.NarratableEntry> narratables() {
            return widgets;
        }

        // ---- helpers ----

        private void resetRow() {
            config.resetToDefault();
            widgets.clear();
            buildWidgets(this.screen);
        }

        // @SuppressWarnings("null"): E is Enum<E> — getValue() is never null; JDT false positive on wildcard capture.
        @SuppressWarnings("null")
        private static Component optionLabel(IConfigOptionList<?> cfg) {
            return Component.translatable(cfg.getValue().name().toLowerCase());
        }

        private static Component boolLabel(boolean v) {
            return v
                ? Component.translatable("options.on").withStyle(ChatFormatting.GREEN)
                : Component.translatable("options.off").withStyle(ChatFormatting.RED);
        }
    }

    // -------------------------------------------------------------------------

    /**
     * A non-interactive row that renders a section header label.
     * Used to separate groups of entries inside a flat list (e.g. vanilla key categories).
     */
    public static class SeparatorRow extends ConfigRow {

        public SeparatorRow(IConfig config, LilConfigScreen screen) {
            super(config, screen);
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                   boolean hovered, float partialTick) {
            int cx = (getContentX() + getContentRight()) / 2;
            int cy = getContentY() + (getContentHeight() - Minecraft.getInstance().font.lineHeight) / 2;
            gfx.centeredText(Minecraft.getInstance().font,
                    I18nHelper.sectionLabel(config)
                              .copy().withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                    cx, cy, -1);
        }
    }
}
