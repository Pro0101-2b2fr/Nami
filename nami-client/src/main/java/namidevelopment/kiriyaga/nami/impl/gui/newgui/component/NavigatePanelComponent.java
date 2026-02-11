package namidevelopment.kiriyaga.nami.impl.gui.newgui.component;

import namidevelopment.kiriyaga.nami.impl.gui.oldgui.screen.ClickGuiScreen;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.newgui.base.PanelRenderer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class NavigatePanelComponent {
    private static final int HEIGHT = 14;
    private static final int PADDING = 6;
    private static final int TOP_OFFSET = 1;

    private final PanelRenderer renderer = new PanelRenderer();
    private final Map<String, Screen> screens = new LinkedHashMap<>();

    private final ColorFeature colorFeature;
    private String activeKey;

    public NavigatePanelComponent() {
        addScreen("ClickGui", CLICK_GUI_SCREEN);
        addScreen("HudEditor", HUD_EDITOR_SCREEN);
        addScreen("Socials", SOCIALS_SCREEN);
        addScreen("Configs", CONFIG_SCREEN);
        addScreen("Plugins", PLUGIN_SCREEN);
        this.colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        resetActive();
    }

    public void addScreen(String name, Screen screen) {
        screens.put(name, screen);
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        context.pose().pushMatrix();
        context.pose().scale(CLICK_GUI_SCREEN.scale, CLICK_GUI_SCREEN.scale);

        int scaledWidth = (int) (MC.getWindow().getGuiScaledWidth() / CLICK_GUI_SCREEN.scale);
        int totalWidth = calcWidth();
        int x = (scaledWidth - totalWidth) / 2;
        int y = TOP_OFFSET;

        renderer.renderPanel(context, x, y, totalWidth, HEIGHT, 0, false);

        int offsetX = x + PADDING;
        for (Map.Entry<String, Screen> entry : screens.entrySet()) {
            String name = entry.getKey();
            boolean active = name.equals(activeKey);

            Color primary = colorFeature.getStyledGlobalColor();
            Color textOff = colorFeature.getStyledTextSecondColor(255);
            Color textCol = active
                    ? colorFeature.getStyledTextColor(255)
                    : textOff;

            int textWidth = FONT_SERVICE.getWidth(name);
            FONT_SERVICE.drawText(
                    context,
                    name,
                    offsetX,
                    (y + Math.ceilDiv(HEIGHT - FONT_SERVICE.getHeight(), 2)),
                    CLICK_GUI_SCREEN.applyFade(toRGBA(textCol)),
                    true
            );

            offsetX += textWidth + PADDING * 2;
        }

        context.pose().popMatrix();
    }

    public void mouseClicked(double mouseX, double mouseY, Font textRenderer) {
        double scaledX = mouseX / CLICK_GUI_SCREEN.scale;
        double scaledY = mouseY / CLICK_GUI_SCREEN.scale;

        int scaledWidth = (int) (MC.getWindow().getGuiScaledWidth() / CLICK_GUI_SCREEN.scale);
        int totalWidth = calcWidth();
        int x = (scaledWidth - totalWidth) / 2;
        int y = TOP_OFFSET;

        int offsetX = x + PADDING;
        for (Map.Entry<String, Screen> entry : screens.entrySet()) {
            String name = entry.getKey();
            Screen screen = entry.getValue();
            int textWidth = FONT_SERVICE.getWidth(name);

            int startX = offsetX;
            int endX = offsetX + textWidth + PADDING * 2;

            if (scaledX >= startX && scaledX <= endX && scaledY >= y && scaledY <= y + HEIGHT) {
                if (!name.equals(activeKey)) {
                    activeKey = name;
                    if (screen instanceof ClickGuiScreen screen1)
                        screen1.setPreviousScreen(MC.screen);
                    MC.setScreen(screen);
                }
                return;
            }
            offsetX = endX;
        }
    }

    public int calcWidth() {
        int width = PADDING;
        for (String key : screens.keySet()) {
            width += FONT_SERVICE.getWidth(key) + PADDING * 2;
        }
        return width;
    }

    public void resetActive() {
        if (screens.isEmpty()) return;
        activeKey = screens.keySet().iterator().next();
    }
}
