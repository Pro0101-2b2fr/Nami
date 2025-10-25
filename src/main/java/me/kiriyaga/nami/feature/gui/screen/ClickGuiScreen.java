package me.kiriyaga.nami.feature.gui.screen;

import me.kiriyaga.nami.feature.gui.components.CategoryPanel;
import me.kiriyaga.nami.feature.gui.components.ModulePanel;
import me.kiriyaga.nami.feature.gui.components.NavigatePanel;
import me.kiriyaga.nami.feature.gui.components.SettingPanel;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.awt.Point;
import java.util.*;
import java.util.stream.Collectors;

import static me.kiriyaga.nami.Nami.*;

public class ClickGuiScreen extends BasePanelScreen {
    public float scale = 1;
    private Screen previousScreen = null;
    private static final long FADE_DURATION_MS = 122L;
    private long fadeStartMs = Util.getMeasuringTimeMs();
    private boolean closing = false;

    private TextFieldWidget searchField;
    private boolean searching = false;

    private ClickGuiModule getClickGuiModule() {
        return MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
    }

    private final List<Text> statusMessages = Arrays.asList(
            Text.literal("Middle-click a module to toggle its drawn state."),
            Text.literal("Middle-click a keybind to switch hold/toggle mode.")
    );

    public ClickGuiScreen() {
        super(Text.literal("NamiGui"));
        initPanels();
    }

    @Override
    protected void initPanels() {
        int x = 20;
        int y = 20;
        for (ModuleCategory moduleCategory : ModuleCategory.getAll()) {
            if ("hud".equalsIgnoreCase(moduleCategory.getName())) continue;
            categoryPositions.putIfAbsent(moduleCategory, new Point(x, y));
            x += CategoryPanel.WIDTH + 1;
            categoryPanels.putIfAbsent(moduleCategory, new CategoryPanel(moduleCategory, expandedModules));
        }
        categoryPositions.keySet().removeIf(cat -> !ModuleCategory.getAll().contains(cat));
        categoryPanels.keySet().removeIf(cat -> !ModuleCategory.getAll().contains(cat));
    }

    @Override
    protected List<Module> getModulesForCategory(ModuleCategory category) {
        return MODULE_MANAGER.getStorage().getByCategory(category);
    }

    @Override
    protected String getSearchText() {
        return searching ? searchField.getText() : "";
    }

    @Override
    protected void init() {
        super.init();
        fadeStartMs = Util.getMeasuringTimeMs();
        closing = false;

        this.searchField = new TextFieldWidget(this.textRenderer, 0, 0, 150, 18, Text.literal("Search"));
        this.searchField.setVisible(false);
        this.addSelectableChild(this.searchField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        checkClose();

        if (previousScreen instanceof TitleScreen
                || previousScreen instanceof DisconnectedScreen
                || previousScreen instanceof MultiplayerScreen) {
            previousScreen.render(context, -1, -1, delta);
        }

        ClickGuiModule clickGuiModule = getClickGuiModule();
        if (clickGuiModule != null && clickGuiModule.background.get()) {
            int alpha = (clickGuiModule.backgroundAlpha.get() & 0xFF) << 24;
            int color = alpha | 0x101010;
            context.fill(0, 0, this.width, this.height, color);
        }

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);

        int panelWidth = NAVIGATE_PANEL.calcWidth();
        int navigateX = (scaledWidth - panelWidth) / 2;
        int navigateY = 1;
        NAVIGATE_PANEL.render(context, this.textRenderer, navigateX, navigateY, mouseX, mouseY);

        int startY = (scaledHeight - 1);
        for (int i = statusMessages.size() - 1; i >= 0; i--) {
            Text message = statusMessages.get(i);
            int textWidth = FONT_MANAGER.getWidth(message);
            int textHeight = FONT_MANAGER.getHeight();

            int x = (int) (scaledWidth - textWidth - 1);
            int y = startY - textHeight;

            FONT_MANAGER.drawText(context, message, x, y, applyFade(0xFFFFFFFF), true);
            startY = y;
        }

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        renderPanels(context, scaledMouseX, scaledMouseY);

        if (clickGuiModule != null && clickGuiModule.descriptions.get()) {
            boolean descriptionRendered = false;
            for (ModuleCategory moduleCategory : categoryPanels.keySet()) {
                Point pos = categoryPositions.get(moduleCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(moduleCategory);
                if (panel == null) continue;

                double scrollOffset = panel.getScrollOffset();

                List<Module> modules = getModulesForCategory(moduleCategory);
                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + ModulePanel.MODULE_SPACING + CategoryPanel.BOTTOM_MARGIN
                        - (int) scrollOffset;

                for (Module module : modules) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;
                    int modY = curY;

                    if (ModulePanel.isHovered(scaledMouseX, scaledMouseY, modX, modY)) {
                        String description = module.getDescription();
                        if (description != null && !description.isEmpty()) {
                            int descX = scaledMouseX + 5;
                            int descY = scaledMouseY;
                            int textWidth = FONT_MANAGER.getWidth(description);
                            int textHeight = 8;

                            context.fill(descX - 2, descY - 2, descX + textWidth + 2, descY + textHeight + 2,
                                    0x7F000000);
                            FONT_MANAGER.drawText(context, description, descX, descY, 0xFFFFFFFF, true);
                        }
                        descriptionRendered = true;
                        break;
                    }

                    curY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;
                    if (expandedModules.contains(module)) {
                        curY += SettingPanel.getSettingsHeight(module);
                    }
                }
                if (descriptionRendered) {
                    break;
                }
            }
        }

        if (this.searching) {
            this.searchField.setX((scaledWidth - this.searchField.getWidth()) / 2);
            this.searchField.setY(scaledHeight - this.searchField.getHeight() - 5);
            this.searchField.render(context, scaledMouseX, scaledMouseY, delta);
        } else {
            Text searchTextHint = Text.literal("Ctrl+F to search for modules");
            int textHeight = FONT_MANAGER.getHeight();
            int x = 1;
            int y = scaledHeight - textHeight - 1;
            FONT_MANAGER.drawText(context, searchTextHint, x, y, applyFade(0xFFFFFFFF), true);
        }

        context.getMatrices().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int i, int j, float f) {
        if (MC.world != null && MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).blur.get())
            this.applyBlur(context);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.searching) {
            if (this.searchField.mouseClicked(mouseX / scale, mouseY / scale, button)) return true;
        }

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        int navX = (int) ((this.width / scale - NAVIGATE_PANEL.calcWidth()) / 2);
        int navY = 1;
        NAVIGATE_PANEL.mouseClicked(scaledMouseX, scaledMouseY, navX, navY, this.textRenderer);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searching) {
            return this.searchField.charTyped(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searching) {
            if (keyCode == 256) { // ESC
                searching = false;
                searchField.setVisible(false);
                setFocused(null);
                return true;
            }
            return searchField.keyPressed(keyCode, scanCode, modifiers);
        }

        if (Screen.hasControlDown() && keyCode == 70) {
            searching = true;
            searchField.setVisible(true);
            searchField.setText("");
            setFocused(searchField);
            return true;
        }

        if (keyCode == MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class).getKeyBind().get() && MC.currentScreen == this && MC.world != null) {
            beginClose();
            return true;
        }
        if (keyCode == 256) {
            beginClose();
            return true;
        }

        if (SettingPanel.keyPressed(keyCode)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        fadeStartMs = Util.getMeasuringTimeMs();
    }

    public Screen getPreviousScreen() {
        return previousScreen;
    }

    public void setPreviousScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    private float getFadeFactor() {
        long elapsed = Util.getMeasuringTimeMs() - fadeStartMs;
        float t = Math.min(1.0f, Math.max(0.0f, elapsed / (float) FADE_DURATION_MS));
        return closing ? (1.0f - t) : t;
    }

    private void checkClose() {
        ClickGuiModule clickGuiModule = getClickGuiModule();
        if (closing && (!clickGuiModule.fade.get() || getFadeFactor() <= 0.0f)) {
            MC.setScreen(null);
        }
    }

    public int applyFade(int argb) {
        if (!getClickGuiModule().fade.get())
            return argb;

        if ((previousScreen == HUD_EDITOR || previousScreen == FRIEND) && MC.currentScreen != this)
            return argb;

        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        float factor = getFadeFactor();
        int newA = Math.round(a * factor);
        if (newA < 0) newA = 0;
        if (newA > a) newA = a;
        return (newA << 24) | rgb;
    }
}
