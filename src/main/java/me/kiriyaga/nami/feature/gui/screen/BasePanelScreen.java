package me.kiriyaga.nami.feature.gui.screen;

import me.kiriyaga.nami.feature.gui.components.CategoryPanel;
import me.kiriyaga.nami.feature.gui.components.ModulePanel;
import me.kiriyaga.nami.feature.gui.components.SettingPanel;
import me.kiriyaga.nami.feature.module.Module;
import me.kiriyaga.nami.feature.module.ModuleCategory;
import me.kiriyaga.nami.feature.module.impl.client.ClickGuiModule;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.awt.Point;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static me.kiriyaga.nami.Nami.MC;
import static me.kiriyaga.nami.Nami.MODULE_MANAGER;

public abstract class BasePanelScreen extends Screen {

    protected final Set<Module> expandedModules = new HashSet<>();
    protected final Map<ModuleCategory, Point> categoryPositions = new HashMap<>();
    protected final Map<ModuleCategory, CategoryPanel> categoryPanels = new HashMap<>();

    protected boolean draggingCategory = false;
    protected ModuleCategory draggedModuleCategory = null;
    private double dragRemainderX = 0.0;
    private double dragRemainderY = 0.0;

    protected BasePanelScreen(Text title) {
        super(title);
    }

    protected abstract void initPanels();

    protected abstract List<Module> getModulesForCategory(ModuleCategory category);

    protected abstract String getSearchText();

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int scaledMouseX = (int) (mouseX / getScale());
        int scaledMouseY = (int) (mouseY / getScale());

        for (ModuleCategory moduleCategory : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            if (CategoryPanel.isHeaderHovered(scaledMouseX, scaledMouseY, pos.x, pos.y)) {
                if (button == 0) {
                    playClickSound();
                    draggingCategory = true;
                    draggedModuleCategory = moduleCategory;
                    return true;
                }
            }
        }

        if (!draggingCategory) {
            for (ModuleCategory moduleCategory : categoryPanels.keySet()) {
                Point pos = categoryPositions.get(moduleCategory);
                if (pos == null) continue;

                CategoryPanel panel = categoryPanels.get(moduleCategory);
                if (panel == null) continue;

                double scrollOffset = panel.getScrollOffset();
                List<Module> modules = getModulesForCategory(moduleCategory);

                int curY = pos.y + CategoryPanel.HEADER_HEIGHT + ModulePanel.MODULE_SPACING + CategoryPanel.BOTTOM_MARGIN - (int) scrollOffset;

                for (Module module : modules) {
                    int modX = pos.x + CategoryPanel.BORDER_WIDTH + SettingPanel.INNER_PADDING;

                    if (ModulePanel.isHovered(scaledMouseX, scaledMouseY, modX, curY)) {
                        if (button == 0) {
                            playClickSound();
                            module.toggle();
                        } else if (button == 1) {
                            if (expandedModules.contains(module)) {
                                expandedModules.remove(module);
                            } else {
                                expandedModules.add(module);
                            }
                            playClickSound();
                        } else if (button == 2) {
                            playClickSound();
                            module.setDrawn(!module.isDrawn());
                        }
                        return true;
                    }

                    curY += ModulePanel.HEIGHT + ModulePanel.MODULE_SPACING;

                    if (expandedModules.contains(module)) {
                        if (SettingPanel.mouseClicked(module, scaledMouseX, scaledMouseY, button, modX, curY)) {
                            return true;
                        }
                        curY += SettingPanel.getSettingsHeight(module);
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingCategory && draggedModuleCategory != null) {
            Point currentPos = categoryPositions.get(draggedModuleCategory);
            if (currentPos != null) {
                double scaledDeltaX = deltaX / getScale();
                double scaledDeltaY = deltaY / getScale();

                double totalDeltaX = scaledDeltaX + dragRemainderX;
                double totalDeltaY = scaledDeltaY + dragRemainderY;

                int intDeltaX = (int) totalDeltaX;
                int intDeltaY = (int) totalDeltaY;

                dragRemainderX = totalDeltaX - intDeltaX;
                dragRemainderY = totalDeltaY - intDeltaY;

                currentPos.translate(intDeltaX, intDeltaY);
                return true;
            }
        }

        SettingPanel.mouseDragged((int) (mouseX / getScale()), (int) (mouseY / getScale()));
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingCategory = false;
        draggedModuleCategory = null;
        dragRemainderX = 0.0;
        dragRemainderY = 0.0;
        SettingPanel.mouseReleased(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int scaledMouseX = (int) (mouseX / getScale());
        int scaledMouseY = (int) (mouseY / getScale());

        for (ModuleCategory moduleCategory : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(moduleCategory);
            if (panel != null && panel.mouseScrolled(scaledMouseX, scaledMouseY, verticalAmount, pos.x, pos.y, this.height)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    protected void renderPanels(DrawContext context, int mouseX, int mouseY) {
        for (ModuleCategory moduleCategory : categoryPanels.keySet()) {
            Point pos = categoryPositions.get(moduleCategory);
            if (pos == null) continue;

            CategoryPanel panel = categoryPanels.get(moduleCategory);
            List<Module> modules = getModulesForCategory(moduleCategory);
            if (panel != null) {
                panel.render(context, this.textRenderer, pos.x, pos.y, mouseX, mouseY, this.height, modules, getSearchText());
            }
        }
    }

    protected void playClickSound() {
        MC.getSoundManager().play(net.minecraft.client.sound.PositionedSoundInstance.master(
                net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK, 1.0f
        ));
    }

    protected float getScale() {
        ClickGuiModule clickGuiModule = MODULE_MANAGER.getStorage().getByClass(ClickGuiModule.class);
        return clickGuiModule != null ? clickGuiModule.scale.get().floatValue() : 1.0f;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
