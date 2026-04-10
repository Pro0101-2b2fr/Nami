package namidevelopment.kiriyaga.nami.impl.gui.base;

import namidevelopment.kiriyaga.nami.impl.gui.widget.ActionWidget;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.api.util.render.ScissorUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class DataPanel<T extends BaseEntry> {
    protected final List<T> entries = new ArrayList<>();
    protected final Function<T, Component> displayMapper;

    protected final PanelRenderer panelRenderer = new PanelRenderer();
    protected final ActionWidget actionWidget = new ActionWidget();

    protected double scrollOffset = 0;
    protected double targetScrollOffset = 0;

    protected int headerHeight = 20;
    protected int inputHeight = 20;

    protected String name;
    protected int x, y, width, height;

    protected boolean dragging = false;
    protected int dragOffsetX = 0;
    protected int dragOffsetY = 0;

    public DataPanel(String name, int x, int y, int width, int height, Function<T, Component> displayMapper) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.displayMapper = displayMapper;
    }

    public void setEntries(List<T> items) {
        entries.clear();
        entries.addAll(items);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        panelRenderer.renderPanel(context, x, y, width, height, headerHeight);
        if (headerHeight > 0) {
            panelRenderer.renderHeaderText(context, textRenderer, name, x, y, headerHeight, 4);
        }

        int contentY = y + headerHeight + 4;
        int lineHeight = getLineHeight();
        int contentHeight = height - headerHeight - inputHeight - 8;
        int maxVisible = contentHeight / lineHeight;

        scrollOffset += (targetScrollOffset - scrollOffset) * 0.3;
        double maxScroll = Math.max(0, entries.size() - maxVisible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        int start = (int) Math.floor(scrollOffset);
        double partialOffset = scrollOffset - start;

        ScissorUtil.enable(context, x, contentY, x + width, contentY + contentHeight);
        int drawY = contentY - (int) (partialOffset * lineHeight);

        for (int i = start; i < Math.min(entries.size(), start + maxVisible + 1); i++) {
            T item = entries.get(i);
            renderEntry(context, item, x, drawY, width, lineHeight, mouseX, mouseY);
            drawY += lineHeight;
        }

        if (entries.size() > maxVisible) {
            int barX = x + width - 3;
            int barY = contentY;
            int barHeight = contentHeight;
            context.fill(barX, barY, barX + 1, barY + barHeight,
                    FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledSecondColor().getRGB());
            float ratio = (float) maxVisible / entries.size();
            int whiteHeight = Math.max((int) (barHeight * ratio), 2);
            int whiteY = (int) (barY + (scrollOffset / maxScroll * (barHeight - whiteHeight)));
            context.fill(barX, whiteY, barX + 1, whiteY + whiteHeight,
                    FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledGlobalColor().getRGB());
        }

        ScissorUtil.disable(context);

        if (actionWidget.isVisible()) {
            actionWidget.render(context, textRenderer, mouseX, mouseY);
        }
    }

    protected void renderEntry(GuiGraphics context, T item, int x, int y, int w, int h, int mouseX, int mouseY) {
        Component display = displayMapper.apply(item);
        if (display != null) {
            FONT_SERVICE.drawText(context, display, x + 4, y + (h - FONT_SERVICE.getHeight()) / 2 + 1, 0xFFFFFFFF,
                    true);
        }
    }

    protected int getLineHeight() {
        return FONT_SERVICE.getHeight() + 4;
    }

    public T getEntryAt(double mouseX, double mouseY) {
        int contentY = y + headerHeight + 4;
        int lineHeight = getLineHeight();
        int contentHeight = height - headerHeight - inputHeight - 8;
        int maxVisible = contentHeight / lineHeight;

        if (mouseX < x || mouseX > x + width || mouseY < contentY || mouseY > contentY + contentHeight)
            return null;

        int start = (int) Math.floor(scrollOffset);
        double partial = scrollOffset - start;
        int drawY = contentY - (int) (partial * lineHeight);

        for (int i = start; i < Math.min(entries.size(), start + maxVisible + 1); i++) {
            if (mouseY >= drawY && mouseY < drawY + lineHeight)
                return entries.get(i);
            drawY += lineHeight;
        }
        return null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        int contentY = y + headerHeight + 4;
        int contentHeight = height - headerHeight - inputHeight - 8;

        if (mouseX >= x && mouseX <= x + width && mouseY >= contentY && mouseY <= contentY + contentHeight) {
            int lineHeight = getLineHeight();
            int maxVisible = contentHeight / lineHeight;
            double maxScroll = Math.max(0, entries.size() - maxVisible);

            targetScrollOffset -= scrollDelta * 1.5;
            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, maxScroll));
            return true;
        }
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (actionWidget.isVisible() && actionWidget.mouseClicked(mouseX, mouseY, button))
            return true;

        if (button == 0 && isHeaderHovered(mouseX, mouseY)) {
            dragging = true;
            dragOffsetX = (int) (mouseX - x);
            dragOffsetY = (int) (mouseY - y);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (dragging) {
            x = (int) (mouseX - dragOffsetX);
            y = (int) (mouseY - dragOffsetY);
            return true;
        }
        return false;
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
    }

    protected boolean isHeaderHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + headerHeight;
    }

    public ActionWidget getActionWidget() {
        return actionWidget;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public int getHeaderHeight() {
        return this.headerHeight;
    }

    public int getInputHeight() {
        return this.inputHeight;
    }

    public double getScrollOffset() {
        return this.scrollOffset;
    }

    public List<T> getEntries() {
        return this.entries;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
