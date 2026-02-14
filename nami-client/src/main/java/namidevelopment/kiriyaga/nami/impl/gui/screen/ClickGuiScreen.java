package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.feature.FeatureCategory;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.CategoryPanel;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.FeaturePanel;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.nami.Nami.NAVIGATE_PANEL;

public class ClickGuiScreen extends NamiScreen {

    private final Map<FeatureCategory, Point> categoryPositions = new HashMap<>();
    private final Map<FeatureCategory, CategoryPanel> categoryPanels = new HashMap<>();

    private boolean draggingCategory = false;
    private FeatureCategory draggedCategory = null;

    private int dragStartX, dragStartY;
    private int initialCategoryX, initialCategoryY;

    public float scale = 1;

    public ClickGuiScreen() {
        super(Component.literal("NamiGuiScreen"));
        initCategories();
    }

    private void initCategories() {
        int startX = 20;
        int startY = 20;

        for (FeatureCategory category : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(category.getName())) continue;

            categoryPositions.put(category, new Point(startX, startY));

            CategoryPanel panel = new CategoryPanel(category.getName());

            for (Feature feature : FEATURE_SERVICE.getStorage().getByCategory(category)) {
                panel.addPanel(new FeaturePanel(feature));
            }

            categoryPanels.put(category, panel);

            startX += CategoryPanel.WIDTH + 2;
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class) != null && FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).background.get()) {
            renderMenuBackground(context);
        }

        NAVIGATE_PANEL.render(context, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);

        context.pose().pushMatrix();
        context.pose().scale(scale, scale);

        for (FeatureCategory category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(category);
            panel.render(context, FONT_SERVICE.rendererProvider.getRenderer(), pos.x, pos.y, scaledMouseX, scaledMouseY);
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
        NAVIGATE_PANEL.mouseClicked(click.x(), click.y(), FONT_SERVICE.rendererProvider.getRenderer());

        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        for (FeatureCategory category : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(category);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(category);

            if (panel.isHeaderHovered(scaledMouseX, scaledMouseY, pos.x, pos.y) && click.button() == 0) {
                draggingCategory = true;
                draggedCategory = category;

                dragStartX = scaledMouseX;
                dragStartY = scaledMouseY;

                initialCategoryX = pos.x;
                initialCategoryY = pos.y;

                return true;
            }
        }

        if (!draggingCategory) {
            for (FeatureCategory category : categoryPanels.keySet()) {
                Point pos = categoryPositions.get(category);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(category);

                if (panel.mouseClicked(scaledMouseX, scaledMouseY, click.button(), pos.x, pos.y)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / scale);
        int scaledMouseY = (int) (mouseY / scale);
        int scaledHeight = (int) (this.height / scale);

        for (FeatureCategory category : FeatureCategory.getAll()) {
            if ("hud".equalsIgnoreCase(category.getName())) continue;

            Point pos = categoryPositions.get(category);
            if (pos == null) continue;
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        int scaledMouseX = (int) (click.x() / scale);
        int scaledMouseY = (int) (click.y() / scale);

        if (draggingCategory && draggedCategory != null) {
            Point pos = categoryPositions.get(draggedCategory);
            if (pos != null) {
                pos.x = initialCategoryX + (scaledMouseX - dragStartX);
                pos.y = initialCategoryY + (scaledMouseY - dragStartY);
                return true;
            }
        }

        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingCategory = false;
        draggedCategory = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
