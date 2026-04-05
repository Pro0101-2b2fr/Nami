package namidevelopment.kiriyaga.nami.impl.gui.widget;

import namidevelopment.kiriyaga.nami.impl.gui.base.PanelRenderer;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class TextBoxWidget {
    private final PanelRenderer panelRenderer = new PanelRenderer();

    private int x, y, width, height;
    private boolean focused = false;

    private final StringBuilder buffer = new StringBuilder();
    private Runnable onEnter;

    public TextBoxWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setOnEnter(Runnable onEnter) {
        this.onEnter = onEnter;
    }

    public void render(GuiGraphics context, Font textRenderer, int mouseX, int mouseY) {
        panelRenderer.renderPanel(context, x, y, width, height, 0, false);
        Color textColor = focused
                ? FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextColor(255)
                : FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class).getStyledTextSecondColor(255);

        String display = buffer +
                (focused && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");

        int textX = x + 4;
        int textY = y + (height - textRenderer.lineHeight) / 2 + 1;

        FONT_SERVICE.drawText(context, Component.nullToEmpty(display), textX, textY, true, toRGBA(textColor));
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            focused = isHovered(mouseX, mouseY);
            return focused;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;
        if (keyCode == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clipboard = MC.keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                clipboard = clipboard.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
                buffer.append(clipboard);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (onEnter != null) onEnter.run();
            focused = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            buffer.setLength(0);
            focused = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !buffer.isEmpty()) {
            buffer.deleteCharAt(buffer.length() - 1);
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;
        if (chr >= 32 && chr != 127) {
            buffer.append(chr);
            return true;
        }
        return false;
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public String getText() {
        return buffer.toString();
    }

    public void clear() {
        buffer.setLength(0);
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }
}
