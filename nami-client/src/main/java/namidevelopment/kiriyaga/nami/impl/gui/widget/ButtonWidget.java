package namidevelopment.kiriyaga.nami.impl.gui.widget;

import namidevelopment.kiriyaga.nami.impl.gui.base.PanelRenderer;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.awt.*;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class ButtonWidget {
    private final String label;
    private final Runnable onClick;
    private final PanelRenderer panelRenderer = new PanelRenderer();

    private int x, y, width, height;
    private boolean active;

    public ButtonWidget(String label, int x, int y, int width, int height, boolean active, Runnable onClick) {
        this.label = label;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.active = active;
        this.onClick = onClick;
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        panelRenderer.renderPanel(context, x, y, width, height, 0, false);


        Color textOff = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextSecondColor(255);
        Color textCol = active ?  FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextColor(255)  : textOff;

        int textWidth = textRenderer.width(label);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - textRenderer.lineHeight) / 2 + 1;

        FONT_SERVICE.drawText(context, Component.nullToEmpty(label), textX, textY, true, toRGBA(textCol));
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0 && isHovered(mouseX, mouseY)) {
            onClick.run();
            return true;
        }
        return false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width
                && mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}