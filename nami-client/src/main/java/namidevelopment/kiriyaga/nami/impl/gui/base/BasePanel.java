package namidevelopment.kiriyaga.nami.impl.gui.base;

import namidevelopment.kiriyaga.api.util.ColorUtils;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.CategoryPanel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.fromRGBA;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;
import static namidevelopment.kiriyaga.api.util.render.RenderUtil.fade;

public abstract class BasePanel {

    protected int x;
    protected int y;
    protected int width;
    public int height;
    public boolean expanded;

    private static final int PADDING = 3;
    private static final int GEAR_PADDING = 5;

    private List<BasePanel> subPanels = new ArrayList<>();
    private float currentHeight = 0;

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        expanded = false;
        this.currentHeight = height;
    }

    public void addSubPanel(BasePanel panel) {
        subPanels.add(panel);
    }

    public List<BasePanel> getSubPanels() {
        return subPanels;
    }

    protected abstract String getName();
    protected abstract boolean isEnabled();

    protected Color getTextColor() {
        return new Color(255, 255, 255, 255);
    }

    protected ColorFeature getColorFeature() {
        return FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
    }

    protected ClickGuiFeature getClickGuiFeature() {
        return FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
    }

    protected Color getEnabledColor() {
        return getColorFeature().getStyledGlobalColor();
    }

    protected Color getDisabledColor() {
        return getColorFeature().getStyledGlobalColor(30);
    }

    public void render(GuiGraphics context, Font font, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY);

        Color baseColor = isEnabled() ? getEnabledColor() : getDisabledColor();
        if (hovered) baseColor = ColorUtils.brighten(baseColor, 20);

        if (getClickGuiFeature().gradientFill.get()) {
            int leftColor = toRGBA(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), baseColor.getAlpha()));
            int rightColor = toRGBA(new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 30));

            fade(context, x, y, x + width, y + height, leftColor, leftColor, rightColor, rightColor);
            context.nextStratum();
        } else {
            context.fill(x, y, x + width, y + height, toRGBA(baseColor));
        }

        int textY = (y + (height - 8) / 2) + 1;
        int textX = x + PADDING + (hovered ? 1 : 0);

        FONT_SERVICE.drawText(context, getName(), textX, textY, toRGBA(getTextColor()), true);

        if (!subPanels.isEmpty()) {
            String gear = expanded ? "-" : "+";
            int gearWidth = FONT_SERVICE.getWidth(gear) + GEAR_PADDING;

            FONT_SERVICE.drawText(context, gear, textX + width - gearWidth, textY, toRGBA(getTextColor()), true);
        }

        if (expanded) {
            int currentY = y + height + PADDING;
            for (BasePanel panel : subPanels) {
                panel.setBounds(x, currentY, width, panel.height);
                panel.render(context, font, mouseX, mouseY);
                currentY += panel.height + PADDING;
            }
        }
    }

    public void onLeftClick() {}
    public void onRightClick() {}
    public void onMiddleClick() {}

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isHovered(mouseX, mouseY)) return false;
        switch (button) {
            case 0 -> onLeftClick();
            case 1 -> onRightClick();
            case 2 -> onMiddleClick();
        }
        return true;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    public boolean isSubPanelHovered(int mouseX, int mouseY) {
        if (expanded) {
            for (BasePanel subPanel : subPanels) {
                if (subPanel.isHovered(mouseX, mouseY)) return true;
            }
        }
        return false;
    }
}
