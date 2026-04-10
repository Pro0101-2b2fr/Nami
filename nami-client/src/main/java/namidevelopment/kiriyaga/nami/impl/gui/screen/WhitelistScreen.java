package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.base.PanelRenderer;
import namidevelopment.kiriyaga.nami.impl.gui.component.panel.WhitelistListPanel;
import namidevelopment.kiriyaga.nami.impl.gui.entry.WhitelistEntry;
import namidevelopment.kiriyaga.nami.impl.gui.widget.TextBoxWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

//this file is 90% AI lol and not very good

public class WhitelistScreen extends NamiScreen {

    private static final int WINDOW_W = 560;
    private static final int WINDOW_H = 320;
    private static final int HEADER_H = 20;
    private static final int SEARCH_H = 18;
    private static final int SEARCH_PADDING = 4;
    private static final int PANEL_GAP = 6;
    private static final int ICON_SIZE = 16;

    private final WhitelistSetting setting;
    private final List<WhitelistEntry> allEntries = new ArrayList<>();
    private final List<WhitelistEntry> filteredAvailable = new ArrayList<>();
    private final WhitelistListPanel availablePanel;
    private final WhitelistListPanel whitelistedPanel;
    private final TextBoxWidget searchBox;
    private final PanelRenderer panelRenderer;

    private int winX, winY;
    private boolean dragging = false;
    private int dragOffX, dragOffY;

    public WhitelistScreen(WhitelistSetting setting, List<String> allIds) {
        super(Component.literal("NamiWhitelist"));
        this.setting = setting;
        this.panelRenderer = new PanelRenderer();
        this.searchBox = new TextBoxWidget(0, 0, 100, SEARCH_H);

        for (String id : allIds) {
            allEntries.add(new WhitelistEntry(id));
        }

        this.availablePanel = new WhitelistListPanel("Available", 0, 0, 0, 0, false, entry -> {
            setting.add(entry.getId());
            applyFilter();
        });

        this.whitelistedPanel = new WhitelistListPanel("Whitelisted", 0, 0, 0, 0, true, entry -> {
            setting.remove(entry.getId());
            applyFilter();
        });

        applyFilter();
    }


    @Override
    protected void init() {
        super.init();
        winX = (this.width - WINDOW_W) / 2;
        winY = (this.height - WINDOW_H) / 2;
    }

    private void applyFilter() {
        String query = searchBox.getText().toLowerCase().trim();
        filteredAvailable.clear();
        for (WhitelistEntry entry : allEntries) {
            if (!setting.contains(entry.getId()) && (query.isEmpty() || entry.getId().contains(query))) {
                filteredAvailable.add(entry);
            }
        }
        availablePanel.setEntries(filteredAvailable);

        List<WhitelistEntry> whitelisted = setting.getWhitelist().stream()
                .map(id -> new WhitelistEntry(id.toString()))
                .sorted(java.util.Comparator.comparing(WhitelistEntry::getId))
                .toList();
        whitelistedPanel.setEntries(whitelisted);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        int halfW = (WINDOW_W - PANEL_GAP) / 2;
        int listH = WINDOW_H - HEADER_H - SEARCH_H - HEADER_H - SEARCH_PADDING * 3;
        int searchY = winY + HEADER_H + SEARCH_PADDING;
        int subHeaderY = searchY + SEARCH_H + SEARCH_PADDING;
        int listY = subHeaderY + HEADER_H;

        ColorFeature cf = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color dimText = cf.getStyledTextSecondColor(180);

        panelRenderer.renderPanel(ctx, winX, winY, WINDOW_W, WINDOW_H, HEADER_H);
        panelRenderer.renderHeaderText(ctx, FONT_SERVICE.rendererProvider.getRenderer(),
                "Whitelist – " + setting.getName(), winX, winY, HEADER_H, 6);

        searchBox.setPosition(winX + 4, searchY);
        searchBox.render(ctx, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        if (searchBox.getText().isEmpty()) {
            FONT_SERVICE.drawText(ctx, "Search...", winX + 8, searchY + (SEARCH_H - 8) / 2 + 1,
                    toRGBA(dimText), false);
        }

        availablePanel.setName("Available (" + filteredAvailable.size() + ")");
        availablePanel.setBounds(winX, subHeaderY, halfW, WINDOW_H - (subHeaderY - winY));
        availablePanel.render(ctx, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        whitelistedPanel.setName("Whitelisted (" + whitelistedPanel.getEntries().size() + ")");
        whitelistedPanel.setBounds(winX + halfW + PANEL_GAP, subHeaderY, halfW, WINDOW_H - (subHeaderY - winY));
        whitelistedPanel.render(ctx, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        super.render(ctx, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        int mx = (int) click.x(), my = (int) click.y();
        int btn = click.button();

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && mx >= winX && mx <= winX + WINDOW_W
                && my >= winY && my <= winY + HEADER_H) {
            dragging = true;
            dragOffX = mx - winX;
            dragOffY = my - winY;
            return true;
        }

        if (searchBox.mouseClicked(mx, my, btn)) return true;

        if (availablePanel.mouseClicked(mx, my, btn)) return true;
        if (whitelistedPanel.mouseClicked(mx, my, btn)) return true;

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            winX = (int) event.x() - dragOffX;
            winY = (int) event.y() - dragOffY;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        availablePanel.mouseReleased(event.x(), event.y(), event.button());
        whitelistedPanel.mouseReleased(event.x(), event.y(), event.button());
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (availablePanel.mouseScrolled(mx, my, v)) return true;
        if (whitelistedPanel.mouseScrolled(mx, my, v)) return true;
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean keyPressed(KeyEvent keyInput) {
        if (keyInput.input() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (searchBox.keyPressed(keyInput.input(), keyInput.scancode(), keyInput.modifiers())) {
            applyFilter();
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean charTyped(CharacterEvent charInput) {
        if (searchBox.charTyped(charInput.codepointAsString().charAt(0), charInput.modifiers())) {
            applyFilter();
            return true;
        }
        return super.charTyped(charInput);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
