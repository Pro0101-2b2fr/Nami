package namidevelopment.kiriyaga.nami.impl.gui.oldgui.components;

import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.util.ColorUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.*;

import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;
import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;

public class FeaturePanel {
    public static final int WIDTH = 100 - CategoryPanel.BORDER_WIDTH * 2 - SettingPanel.INNER_PADDING * 2;
    public static final int HEIGHT = 13;
    public static final int PADDING = 3;
    public static final int Feature_SPACING = 1;
    private static final int GEAR_PADDING = 5;

    private final Feature Feature;

    public FeaturePanel(Feature Feature) {
        this.Feature = Feature;
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y) {
        return mouseX >= x && mouseX <= x + WIDTH && mouseY >= y && mouseY <= y + HEIGHT;
    }

    private ClickGuiFeature getClickGuiFeature() {
        return FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
    }

    private ColorFeature getColorFeature() {
        return FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
    }

    public void render(GuiGraphics context,Font textRenderer, int x, int y, int mouseX, int mouseY) {
        boolean hovered = isHovered(mouseX, mouseY, x, y);

        Color primary = getColorFeature().getStyledGlobalColor();
        Color second = getColorFeature().getStyledGlobalColor(30);
        Color fillCol = Feature.isEnabled() ? primary : second;
        Color textCol = new Color(255, 255, 255, 255);

        if (hovered)
            fillCol = ColorUtils.brighten(fillCol, 20);

        context.fill(x, y, x + WIDTH, y + HEIGHT, CLICK_GUI_SCREEN.applyFade(toRGBA(fillCol)));

        int textY = (y + (HEIGHT - 8) / 2) + 1;
        int baseTextX = x + PADDING + (hovered ? 1 : 0);
        FONT_SERVICE.drawText(context, Feature.getName(), baseTextX, textY, CLICK_GUI_SCREEN.applyFade(toRGBA(textCol)), true);

        ClickGuiFeature clickGuiFeature = getClickGuiFeature();
        if (clickGuiFeature != null && clickGuiFeature.gear.get()) {
            boolean expanded = Feature.isExpanded();
            String gear = expanded ? "-" : "+";
            int gearWidth = FONT_SERVICE.getWidth(gear) + GEAR_PADDING;

            FONT_SERVICE.drawText(context, gear, baseTextX + WIDTH - gearWidth, textY, CLICK_GUI_SCREEN.applyFade(toRGBA(textCol)),true);
        }
    }
}