package namidevelopment.kiriyaga.nami.impl.gui.base;


import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;


import java.awt.*;


import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;


public class PanelRenderer {
    private final ColorFeature colorFeature;
    private final ClickGuiFeature clickGuiFeature;
    public PanelRenderer() {
        this.colorFeature = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        this.clickGuiFeature = FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class);
    }

    public void renderPanel(GuiGraphics context, int x, int y, int width, int height, int headerHeight, boolean renderHeader, boolean renderBackground) {

        Color primary = colorFeature.getStyledGlobalColor();
        Color secondary = colorFeature.getStyledSecondColor();

        if (renderBackground) {
            int bgColor = toRGBA(new Color(30, 30, 30, clickGuiFeature.guiAlpha.get()));
            context.fill(x, y, x + width, y + height, bgColor);
        }

        int lineColor;
        if (clickGuiFeature.lines.get()) {
            lineColor = primary.getRGB();
        } else {
            lineColor = new Color(20, 20, 20, 0).getRGB();
        }

        if (!renderHeader) {
            context.fill(x, y, x + width, y + 1, lineColor);
        }

        if (renderHeader && headerHeight > 0) {
            context.fill(x, y + headerHeight, x + width, y + headerHeight + 1, lineColor);
        }

        context.fill(x, y + height - 1, x + width, y + height, lineColor);

        int topOffset = (renderHeader && headerHeight > 0) ? headerHeight + 1 : 1;
        context.fill(x, y + topOffset, x + 1, y + height - 1, lineColor);
        context.fill(x + width - 1, y + topOffset, x + width, y + height - 1, lineColor);

        if (renderHeader && headerHeight > 0) {
            context.fill(x, y, x + width, y + headerHeight, toRGBA(primary));
        }
    }

    public void renderHeaderText(GuiGraphics context, Font textRenderer, String text, int x, int y, int headerHeight, int padding) {
        if (text == null || textRenderer == null) return; // ???

        Color textCol =  FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextColor(255);

        int textY = y + (headerHeight - textRenderer.lineHeight) / 2;
        FONT_SERVICE.drawText(context, text, x + padding, textY + 1, toRGBA(textCol), true);
    }


    public void renderPanel(GuiGraphics context, int x, int y, int width, int height, int headerHeight) {
        renderPanel(context, x, y, width, height, headerHeight, true, true);
    }

    public void renderPanel(GuiGraphics context, int x, int y, int width, int height, int headerHeight, boolean renderHeader) {
        renderPanel(context, x, y, width, height, headerHeight, renderHeader, true);
    }
}