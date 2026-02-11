package namidevelopment.kiriyaga.nami.impl.gui.oldgui.screen;

import namidevelopment.kiriyaga.nami.impl.gui.oldgui.components.CategoryPanel;
import namidevelopment.kiriyaga.nami.impl.gui.oldgui.components.FeaturePanel;
import namidevelopment.kiriyaga.nami.impl.gui.oldgui.components.SettingPanel;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.util.*;
import java.awt.Point;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
public class ClickGuiScreen extends Screen {
    private final Map<FeatureCategory, Point> categoryPositions = new HashMap<>();
    private final Map<FeatureCategory, CategoryPanel> categoryPanels = new HashMap<>();
    private boolean draggingCategory = false;
    private FeatureCategory draggedFeatureCategory = null;
    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;
    public float scale = 1;
    private Screen previousScreen = null;
    private static final long FADE_DURATION_MS = 122L;
    private long fadeStartMs = Util.getMillis();
    private boolean closing = false;

    private ClickGuiFeature getClickGuiFeature() {
        return FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
    }

    private final List<Component> statusMessages = Arrays.asList(
            Component.literal("Middle-click a Feature to toggle its drawn state."),
            Component.literal("Middle-click a keybind to switch hold/toggle mode.")
    );

    public ClickGuiScreen() {
        super(Component.literal("NamiGui"));
        syncCategoryPositions();
        initCategoryPanels();
    }

    private void syncCategoryPositions() {
        int x = 20;
        int y = 20;
        for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;
            categoryPositions.putIfAbsent(FeatureCategory, new Point(x, y));
            x += CategoryPanel.WIDTH + 1;
        }
        categoryPositions.keySet().removeIf(cat -> !FeatureCategory.getAll().contains(cat));
    }

    private void initCategoryPanels() {
        for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;
            categoryPanels.putIfAbsent(FeatureCategory,
                    new CategoryPanel(FeatureCategory));
        }
        categoryPanels.keySet().removeIf(cat -> !FeatureCategory.getAll().contains(cat));
    }

    @Override
    protected void init() {
        super.init();
        fadeStartMs = Util.getMillis();
        closing = false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        checkClose();
        syncCategoryPositions();

        if (previousScreen instanceof TitleScreen
                || previousScreen instanceof DisconnectedScreen
                || previousScreen instanceof JoinMultiplayerScreen) {
            previousScreen.render(context, -1, -1, delta);
        }

        ClickGuiFeature clickGuiFeature = getClickGuiFeature();
        if (clickGuiFeature != null && clickGuiFeature.background.get()) {
            renderMenuBackground(context);
/*            int alpha = (clickGuiFeature.backgroundAlpha.get() & 0xFF) << 24;
            int color = alpha | (Feature_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor().getRGB() & 0xFFFFFF);
            context.fill(0, 0, this.width, this.height, applyFade(color));*/
        }

        NAVIGATE_PANEL.render(context, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        int scaledWidth = (int) (this.width / scale);
        int scaledHeight = (int) (this.height / scale);

        int startY = (scaledHeight - 1);
        for (int i = statusMessages.size() - 1; i >= 0; i--) {
            Component message = statusMessages.get(i);
            int textWidth = FONT_SERVICE.getWidth(message);
            int textHeight = FONT_SERVICE.getHeight();

            int x = (int) (scaledWidth - textWidth - 1);
            int y = startY - textHeight;

            FONT_SERVICE.drawText(context, message, x, y, applyFade(FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextColor(255).getRGB()), true);
            startY = y;
        }

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;

            Point pos = categoryPositions.get(FeatureCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(FeatureCategory);
            if (panel != null)
                panel.render(context, FONT_SERVICE.rendererProvider.getRenderer(), pos.x, pos.y, scaledMouseX, scaledMouseY, scaledHeight);
        }

        if (clickGuiFeature != null && clickGuiFeature.descriptions.get()) {
            for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
                if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;

                Point pos = categoryPositions.get(FeatureCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(FeatureCategory);
                if (panel == null) continue;

                if (!panel.isMouseOverContent(scaledMouseX, scaledMouseY, pos.x, pos.y, scaledHeight)) {
                    continue;
                }

                double scrollOffset = panel.getScrollOffset();

                List<Feature> Features = FEATURE_SERVICE.getStorage().getByCategory(FeatureCategory);
                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + FeaturePanel.Feature_SPACING + CategoryPanel.BOTTOM_MARGIN
                        - (int) scrollOffset;

                for (Feature Feature : Features) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;
                    int modY = curY;

                    if (FeaturePanel.isHovered(scaledMouseX, scaledMouseY, modX, modY)) {
                        String description = Feature.getDescription();
                        if (description != null && !description.isEmpty()) {
                            int descX = scaledMouseX + 5;
                            int descY = scaledMouseY;
                            int textWidth = FONT_SERVICE.getWidth(description);
                            int textHeight = 8;

                            context.fill(descX - 2, descY - 2, descX + textWidth + 2, descY + textHeight + 2, 0x7F000000);
                            FONT_SERVICE.drawText(context, description, descX, descY, applyFade(FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextColor(255).getRGB()), true);
                        }
                        context.pose().popMatrix();
                        return;
                    }

                    curY += FeaturePanel.HEIGHT + FeaturePanel.Feature_SPACING;
                    if (Feature.isExpanded()) {
                        curY += SettingPanel.getSettingsHeight(Feature);
                    }
                }
            }
        }

        context.pose().popMatrix();
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(GuiGraphics context, int i, int j, float f) {
        if (MC.level != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).blur.get())
            this.renderBlurredBackground(context);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        syncCategoryPositions();

        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);
        int scaledHeight = (int) (this.height / scale);

        NAVIGATE_PANEL.mouseClicked(click.x(), click.y(), FONT_SERVICE.rendererProvider.getRenderer());

        for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;

            Point pos = categoryPositions.get(FeatureCategory);
            if (pos == null) continue;

            if (CategoryPanel.isHeaderHovered(scaledMouseX, scaledMouseY, pos.x, pos.y)) {
                if (click.button() == 0) {
                    playClickSound();
                    draggingCategory = true;
                    draggedFeatureCategory = FeatureCategory;
                    dragStartX = scaledMouseX;
                    dragStartY = scaledMouseY;
                    initialCategoryX = pos.x;
                    initialCategoryY = pos.y;
                    return true;
                }
            }
        }

        if (!draggingCategory) {
            for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
                if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;

                Point pos = categoryPositions.get(FeatureCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(FeatureCategory);
                if (panel == null) continue;

                if (!panel.isMouseOverContent(scaledMouseX, scaledMouseY, pos.x, pos.y, scaledHeight)) {
                    continue;
                }

                double scrollOffset = panel.getScrollOffset();

                List<Feature> Features = FEATURE_SERVICE.getStorage().getByCategory(FeatureCategory);

                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + FeaturePanel.Feature_SPACING + CategoryPanel.BOTTOM_MARGIN
                        - (int) scrollOffset;

                for (Feature Feature : Features) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;

                    if (FeaturePanel.isHovered(scaledMouseX, scaledMouseY, modX, curY)) {
                        if (click.button() == 0) {
                            playClickSound();
                            Feature.toggle();
                        } else if (click.button() == 1) {
                            if (Feature.isExpanded()) {
                                Feature.setExpanded(false);
                            } else {
                                Feature.setExpanded(true);
                            }
                            playClickSound();
                        } else if (click.button() == 2) {
                            playClickSound();
                            Feature.setDrawn(!Feature.isDrawn());
                        }
                        return true;
                    }

                    curY += FeaturePanel.HEIGHT + FeaturePanel.Feature_SPACING;

                    if (Feature.isExpanded()) {
                        if (SettingPanel.mouseClicked(Feature, scaledMouseX, scaledMouseY, click.button(), modX, curY)) {
                            return true;
                        }
                        curY += SettingPanel.getSettingsHeight(Feature);
                    }
                }
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).getKeyBind().get() && MC.screen == CLICK_GUI_SCREEN && MC.level != null) {
            beginClose();
            return true;
        }
        if (keyInput.input() == 256) {
            beginClose();
            return true;
        }

        if (SettingPanel.keyPressed(keyInput.input())) return true;
        return super.keyPressed(keyInput);
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        fadeStartMs = Util.getMillis();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double d, double e) {
        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        if (draggingCategory && draggedFeatureCategory != null) {
            Point currentPos = categoryPositions.get(draggedFeatureCategory);
            if (currentPos != null) {
                currentPos.x = initialCategoryX + (scaledMouseX - dragStartX);
                currentPos.y = initialCategoryY + (scaledMouseY - dragStartY);
                return true;
            }
        }

        SettingPanel.mouseDragged(scaledMouseX, scaledMouseY);
        return super.mouseDragged(click,  d,  e);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingCategory = false;
        draggedFeatureCategory = null;
        SettingPanel.mouseReleased(click);
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledHeight = (int) (this.height / scale);

        for (FeatureCategory FeatureCategory : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(FeatureCategory.getName())) continue;

            Point pos = categoryPositions.get(FeatureCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(FeatureCategory);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y, scaledHeight)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void playClickSound() {
        MC.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0f
        ));
    }

    public Screen getPreviousScreen() {
        return previousScreen;
    }

    public void setPreviousScreen(Screen previousScreen) {
        this.previousScreen = previousScreen;
    }

    private float getFadeFactor() {
        long elapsed = Util.getMillis() - fadeStartMs;
        float t = Math.min(1.0f, Math.max(0.0f, elapsed / (float) FADE_DURATION_MS));
        return closing ? (1.0f - t) : t;
    }

    private void checkClose() { // shitcode ikik
        ClickGuiFeature clickGuiFeature = FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
        if (!closing) return;

        if (clickGuiFeature.fade.get()) {
            if (getFadeFactor() <= 0.0f) {
                MC.setScreen(null);
            }
        } else {
            MC.setScreen(null);
        }
    }

    public int applyFade(int argb) {
        if (!FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).fade.get())
            return argb;

        if ((previousScreen == HUD_EDITOR_SCREEN || previousScreen == SOCIALS_SCREEN) && MC.screen != CLICK_GUI_SCREEN)
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
