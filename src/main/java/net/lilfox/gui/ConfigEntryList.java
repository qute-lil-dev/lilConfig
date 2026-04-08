package net.lilfox.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.lilfox.config.*;
import net.lilfox.hotkey.KeyBind;
import net.lilfox.manager.LilConfigManager;
import net.lilfox.manager.LilConfigManager.ConflictEntry;
import net.lilfox.util.I18nHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import org.jspecify.annotations.NonNull;

/**
 * Scrollable list that renders one row per {@link IConfig} entry.
 *
 * <p>Row layout (widths in logical px, constant across scales):
 * <pre>
 *   |←LABEL_ZONE_W→|←──WIDGET_ZONE──→| [T?|Effect?] [Reset]
 *                  |←HOTKEY_BTN_W→|4|←TOGGLE_BTN_W→|
 * </pre>
 * Labels are drawn at the content left edge. Widgets start at a fixed
 * column offset ({@code LABEL_ZONE_W}) so all rows align.
 * The secondary column (mode-toggle or effect button) and reset column
 * are at fixed X positions so Reset is the same X for every row type.
 *
 * <p>Button <em>height</em> scales sub-proportionally with GUI scale so that
 * physical height grows slower than physical width:
 * physical&nbsp;h(N)&nbsp;≈&nbsp;BASE_BTN_H&nbsp;×&nbsp;(0.75&nbsp;+&nbsp;0.25N).
 * Example: scale&nbsp;1&nbsp;→&nbsp;20&nbsp;px; scale&nbsp;2&nbsp;→&nbsp;25&nbsp;px physical.
 */
public class ConfigEntryList extends ContainerObjectSelectionList<ConfigEntryList.ConfigRow> {

    // ----- layout constants (logical px, not affected by GUI scale) -----
    static final int HOTKEY_BTN_W  = 60;
    static final int TOGGLE_BTN_W  = 28;
    static final int ROW_PADDING   = 4;
    /** Total widget zone width; matches the two-button HOTKEYED_BOOLEAN layout exactly. */
    static final int WIDGET_ZONE   = HOTKEY_BTN_W + ROW_PADDING + TOGGLE_BTN_W;
    /**
     * Width of the secondary-button slot (mode-toggle or effect button).
     * Sized to {@code EFFECT_BTN_W} so the Reset column is identical for all row types,
     * even when only a small mode-toggle button occupies the slot.
     */
    static final int SECONDARY_W   = 36;
    static final int RESET_BTN_W   = 36;
    /** Base button height at GUI scale 1 (physical px = logical px at scale 1). */
    private static final int BASE_BTN_H   = 20;
    /** Fixed label column width. */
    static final int LABEL_ZONE_W  = 80;

    /**
     * Button height in logical pixels, computed so that physical height grows
     * sub-proportionally with GUI scale N:
     * <pre>logical_h(N) = BASE_BTN_H × (0.75/N + 0.25)</pre>
     * This yields physical_h(1)=20, physical_h(2)≈25, physical_h(3)=30.
     * Minimum 12 px to keep text readable at large scales.
     */
    static int btnH() {
        int scale = (int) Minecraft.getInstance().getWindow().getGuiScale();
        return Math.max(12, Math.round(BASE_BTN_H * (0.75f / scale + 0.25f)));
    }

    /** Mode-toggle button is square: width equals height. */
    static int modeBtnW() { return btnH(); }

    private final LilConfigScreen owner;

    public ConfigEntryList(LilConfigScreen screen, ConfigGroup group) {
        super(
            Minecraft.getInstance(),
            screen.width,
            screen.height - LilConfigScreen.TAB_BAR_H - LilConfigScreen.FOOTER_H,
            LilConfigScreen.TAB_BAR_H,
            btnH() + 8
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
        return owner.width - 80;
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
        /** Mode-toggle (T/S) button, present only for INTEGER and bounded DOUBLE. */
        private Button modeButton;
        /** Optional effect button supplied by the config. */
        private Button effectButton;
        private Button hotkeyButton;
        private boolean sliderMode = true;

        public ConfigRow(IConfig config, LilConfigScreen screen) {
            this.config = config;
            this.screen = screen;
            buildWidgets(screen);
        }

        private void buildWidgets(LilConfigScreen screen) {
            // placeholder x/y; repositionWidgets() positions them each frame
            modeButton = null;
            effectButton = null;
            switch (config.getType()) {
                case BOOLEAN -> {
                    IConfigBoolean b = (IConfigBoolean) config;
                    Button.Builder tb = Button.builder(boolLabel(b.getValue()),
                            btn -> { b.setValue(!b.getValue()); btn.setMessage(boolLabel(b.getValue())); })
                            .size(WIDGET_ZONE, btnH()).pos(0, 0);
                    Component descTip = I18nHelper.desc(config);
                    if (descTip != null) tb.tooltip(Tooltip.create(descTip));
                    widgets.add(tb.build());
                }
                case HOTKEYED_BOOLEAN -> {
                    IConfigHotkey  hk = (IConfigHotkey)  config;
                    IConfigBoolean b  = (IConfigBoolean) config;
                    hotkeyButton = Button.builder(keyLabel(hk.getKeyBind(), hk),
                            btn -> screen.startRebind(hk, btn))
                            .size(HOTKEY_BTN_W, btnH()).pos(0, 0).build();
                    Button toggleBtn = Button.builder(boolLabel(b.getValue()),
                            btn -> { b.setValue(!b.getValue()); btn.setMessage(boolLabel(b.getValue())); })
                            .size(TOGGLE_BTN_W, btnH()).pos(0, 0).build();
                    widgets.add(hotkeyButton);
                    widgets.add(toggleBtn);
                }
                case HOTKEY -> {
                    IConfigHotkey hk = (IConfigHotkey) config;
                    hotkeyButton = Button.builder(keyLabel(hk.getKeyBind(), hk),
                            btn -> screen.startRebind(hk, btn))
                            .size(WIDGET_ZONE, btnH()).pos(0, 0).build();
                    widgets.add(hotkeyButton);
                }
                case INTEGER -> {
                    ConfigInteger ci = (ConfigInteger) config;
                    if (sliderMode) {
                        IntSlider slider = new IntSlider(0, 0, WIDGET_ZONE, btnH(), ci);
                        Component inputTip = I18nHelper.inputDesc(config);
                        if (inputTip != null) slider.setTooltip(Tooltip.create(inputTip));
                        else slider.setTooltip(Tooltip.create(Component.translatable(
                                "lilconfig.tooltip.range", ci.getMinValue(), ci.getMaxValue())));
                        widgets.add(slider);
                    } else {
                        EditBox box = new EditBox(Minecraft.getInstance().font,
                                0, 0, WIDGET_ZONE, btnH(), Component.literal(ci.getName()));
                        box.setValue(String.valueOf(ci.getValue()));
                        box.setResponder(s -> {
                            try { ci.setValue(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
                        });
                        Component inputTip = I18nHelper.inputDesc(config);
                        if (inputTip != null) box.setTooltip(Tooltip.create(inputTip));
                        else box.setTooltip(Tooltip.create(Component.translatable(
                                "lilconfig.tooltip.range", ci.getMinValue(), ci.getMaxValue())));
                        widgets.add(box);
                    }
                    modeButton = Button.builder(
                            Component.literal(sliderMode ? "T" : "S"),
                            btn -> { sliderMode = !sliderMode; rebuildWidgets(); })
                            .size(modeBtnW(), btnH()).pos(0, 0).build();
                    widgets.add(modeButton);
                }
                case STRING -> {
                    ConfigString cs = (ConfigString) config;
                    EditBox box = new EditBox(Minecraft.getInstance().font,
                            0, 0, WIDGET_ZONE, btnH(),
                            I18nHelper.label(config));
                    box.setValue(cs.getValue());
                    box.setResponder(cs::setValue);
                    Component inputTip = I18nHelper.inputDesc(config);
                    if (inputTip != null) box.setTooltip(Tooltip.create(inputTip));
                    widgets.add(box);
                }
                case OPTION_LIST -> {
                    IConfigOptionList<?> cfg = (IConfigOptionList<?>) config;
                    Button.Builder ob = Button.builder(
                                    optionLabel(cfg),
                                    b -> { cfg.cycle(); b.setMessage(optionLabel(cfg)); })
                            .size(WIDGET_ZONE, btnH()).pos(0, 0);
                    Component descTip = I18nHelper.desc(config);
                    if (descTip != null) ob.tooltip(Tooltip.create(descTip));
                    widgets.add(ob.build());
                }
                case DOUBLE -> {
                    ConfigDouble cd = (ConfigDouble) config;
                    boolean bounded = Double.isFinite(cd.getMin()) && Double.isFinite(cd.getMax());
                    if (bounded && sliderMode) {
                        DoubleSlider slider = new DoubleSlider(0, 0, WIDGET_ZONE, btnH(), cd);
                        Component inputTip = I18nHelper.inputDesc(config);
                        if (inputTip != null) slider.setTooltip(Tooltip.create(inputTip));
                        else slider.setTooltip(Tooltip.create(Component.translatable(
                                "lilconfig.tooltip.range", cd.getMin(), cd.getMax())));
                        widgets.add(slider);
                    } else {
                        EditBox box = new EditBox(Minecraft.getInstance().font,
                                0, 0, WIDGET_ZONE, btnH(), Component.literal(cd.getName()));
                        box.setValue(String.valueOf(cd.getValue()));
                        box.setResponder(s -> {
                            try { cd.setValue(Double.parseDouble(s.trim())); } catch (NumberFormatException ignored) {}
                        });
                        Component inputTip = I18nHelper.inputDesc(config);
                        if (inputTip != null) box.setTooltip(Tooltip.create(inputTip));
                        else if (Double.isFinite(cd.getMin()) || Double.isFinite(cd.getMax())) {
                            box.setTooltip(Tooltip.create(Component.translatable(
                                    "lilconfig.tooltip.range", cd.getMin(), cd.getMax())));
                        }
                        widgets.add(box);
                    }
                    if (bounded) {
                        modeButton = Button.builder(
                                Component.literal(sliderMode ? "T" : "S"),
                                btn -> { sliderMode = !sliderMode; rebuildWidgets(); })
                                .size(modeBtnW(), btnH()).pos(0, 0).build();
                        widgets.add(modeButton);
                    }
                }
                case SEPARATOR -> { return; }
            }

            // optional effect button — occupies the same secondary slot as mode-toggle
            if (config.getEffectButtonLabel() != null) {
                Runnable action = config.getEffectAction();
                effectButton = Button.builder(Component.translatable(config.getEffectButtonLabel()),
                        btn -> { if (action != null) action.run(); })
                        .size(SECONDARY_W, btnH()).pos(0, 0).build();
                widgets.add(effectButton);
            }

            // reset button
            Button resetBtn = Button.builder(Component.translatable("lilconfig.reset"), btn -> resetRow())
                    .tooltip(Tooltip.create(Component.translatable("lilconfig.tooltip.reset")))
                    .size(RESET_BTN_W, btnH()).pos(0, 0).build();
            widgets.add(resetButton = resetBtn);
        }

        /** Clears and rebuilds widgets without touching the config value. Used by the mode-toggle button. */
        private void rebuildWidgets() {
            widgets.clear();
            buildWidgets(this.screen);
        }

        /**
         * Repositions all widgets based on the entry's current content bounds.
         *
         * <p>Main widgets cascade from {@code LABEL_ZONE_W}.
         * The secondary button (mode-toggle or effect) is at a fixed column immediately
         * after the widget zone. Reset is at the next fixed column after that.
         * This keeps the Reset column identical for every row type.
         */
        private void repositionWidgets() {
            int cy = getContentYMiddle() - btnH() / 2;

            // fixed secondary column: right after widget zone
            int secondaryX = getContentX() + LABEL_ZONE_W + WIDGET_ZONE + ROW_PADDING;
            if (modeButton != null) {
                modeButton.setX(secondaryX);
                modeButton.setY(cy);
            }
            if (effectButton != null) {
                effectButton.setX(secondaryX);
                effectButton.setY(cy);
            }

            // fixed reset column: always SECONDARY_W + gap after secondary column
            if (resetButton != null) {
                resetButton.setX(secondaryX + SECONDARY_W + ROW_PADDING);
                resetButton.setY(cy);
            }

            // main widgets cascade from LABEL_ZONE_W (skip secondary and reset)
            int x = getContentX() + LABEL_ZONE_W;
            for (AbstractWidget w : widgets) {
                if (w == modeButton || w == effectButton || w == resetButton) continue;
                w.setX(x);
                w.setY(cy);
                x += w.getWidth() + ROW_PADDING;
            }
        }

        @Override
        public void extractContent(GuiGraphicsExtractor gfx, int mouseX, int mouseY, boolean hovered, float partialTick) {
            repositionWidgets();
            if (resetButton != null) resetButton.active = config.isModified();
            if (hotkeyButton != null && !screen.isRebinding(config)) {
                IConfigHotkey hk = (IConfigHotkey) config;
                hotkeyButton.setMessage(keyLabel(hk.getKeyBind(), hk));
                List<ConflictEntry> conflicts = LilConfigManager.getInstance().getConflicts(hk.getKeyBind(), hk);
                hotkeyButton.setTooltip(Tooltip.create(buildHotkeyTooltip(hk, conflicts)));
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
            rebuildWidgets();
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

        private static Component buildHotkeyTooltip(IConfigHotkey hk, List<ConflictEntry> conflicts) {
            MutableComponent tip = Component.empty();
            Component hotkeyDesc = I18nHelper.hotkeyDesc((IConfig) hk);
            if (hotkeyDesc != null) {
                tip.append(hotkeyDesc).append("\n");
            }
            tip.append(Component.translatable("lilconfig.tooltip.rebind"));

            if (conflicts.isEmpty()) return tip;

            tip.append("\n");
            long wh = Minecraft.getInstance().getWindow().handle();
            boolean shiftDown = KeyBind.isKeyHeld(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT), wh)
                    || KeyBind.isKeyHeld(com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_RIGHT_SHIFT), wh);
            if (!shiftDown) {
                tip.append("\n").append(
                    Component.translatable("lilconfig.tooltip.conflicts.hint")
                             .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return tip;
            }

            tip.append("\n").append(
                Component.translatable("lilconfig.tooltip.conflicts")
                         .withStyle(ChatFormatting.WHITE));

            // Group by mod name (preserving insertion order)
            SequencedMap<String, List<ConflictEntry>> byMod = new LinkedHashMap<>();
            for (ConflictEntry e : conflicts) {
                byMod.computeIfAbsent(e.modName(), k -> new ArrayList<>()).add(e);
            }

            boolean firstMod = true;
            for (Map.Entry<String, List<ConflictEntry>> modEntry : byMod.entrySet()) {
                if (!firstMod) {
                    tip.append("\n").append(Component.literal("-----").withStyle(ChatFormatting.DARK_GRAY));
                }
                firstMod = false;
                tip.append("\n").append(
                    Component.literal(modEntry.getKey()).withStyle(ChatFormatting.GOLD));
                for (ConflictEntry e : modEntry.getValue()) {
                    tip.append("\n  - ").append(
                        e.entryLabel().copy()
                         .append(Component.literal(" [" + e.keyDisplay() + "]"))
                         .withStyle(ChatFormatting.GOLD));
                }
            }
            return tip;
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

    // -------------------------------------------------------------------------

    private static final class IntSlider extends AbstractSliderButton {
        private final ConfigInteger ci;

        IntSlider(int x, int y, int w, int h, ConfigInteger ci) {
            super(x, y, w, h,
                  Component.literal(String.valueOf(ci.getValue())),
                  ci.getMaxValue() == ci.getMinValue() ? 0.0
                      : (double)(ci.getValue() - ci.getMinValue()) / (ci.getMaxValue() - ci.getMinValue()));
            this.ci = ci;
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.literal(String.valueOf(ci.getValue())));
        }

        @Override
        protected void applyValue() {
            ci.setValue((int) Math.round(ci.getMinValue() + value * (ci.getMaxValue() - ci.getMinValue())));
            updateMessage();
        }
    }

    private static final class DoubleSlider extends AbstractSliderButton {
        private final ConfigDouble cd;

        DoubleSlider(int x, int y, int w, int h, ConfigDouble cd) {
            super(x, y, w, h,
                  fmtMsg(cd),
                  (cd.getValue() - cd.getMin()) / (cd.getMax() - cd.getMin()));
            this.cd = cd;
        }

        @Override
        protected void updateMessage() {
            setMessage(fmtMsg(cd));
        }

        @Override
        protected void applyValue() {
            cd.setValue(cd.getMin() + value * (cd.getMax() - cd.getMin()));
            updateMessage();
        }

        private static Component fmtMsg(ConfigDouble cd) {
            return Component.literal(String.format("%.4g", cd.getValue()));
        }
    }
}
