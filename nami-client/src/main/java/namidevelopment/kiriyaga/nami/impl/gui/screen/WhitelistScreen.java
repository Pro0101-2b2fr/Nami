package namidevelopment.kiriyaga.nami.impl.gui.screen;

import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.api.util.render.ScissorUtil;
import namidevelopment.kiriyaga.nami.impl.feature.client.ClickGuiFeature;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.NamiScreen;
import namidevelopment.kiriyaga.nami.impl.gui.base.PanelRenderer;
import namidevelopment.kiriyaga.nami.impl.gui.widget.TextBoxWidget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final int ROW_H = 16;
    private static final int SCROLLBAR_W = 3;
    private static final int ICON_SIZE = 16;
    private static final int ICON_OFFSET = ICON_SIZE + 2; // horizontal space icon takes

    private final WhitelistSetting setting;
    private final List<String> allIds;
    private final List<String> filteredAvailable = new ArrayList<>();

    private double scrollAvailable = 0;
    private double scrollWhitelisted = 0;
    private double targetScrollAvailable = 0;
    private double targetScrollWhitelisted = 0;
    private boolean draggingScrollLeft = false;
    private boolean draggingScrollRight = false;
    private int scrollDragStartY = 0;
    private double scrollDragStartVal = 0;

    private String lastSearchQuery = "";


    private final Map<String, ItemStack> itemIconCache = new HashMap<>();
    private final TextBoxWidget searchBox;
    private final PanelRenderer panelRenderer;
    private int winX, winY;
    private boolean dragging = false;
    private int dragOffX, dragOffY;
    private int halfW;
    private int leftX, rightX;
    private int listY, listH;
    private int searchY;

    public WhitelistScreen(WhitelistSetting setting, List<String> allIds) {
        super(Component.literal("NamiWhitelist"));
        this.setting = setting;
        this.allIds = new ArrayList<>(allIds);
        this.panelRenderer = new PanelRenderer();
        this.searchBox = new TextBoxWidget(0, 0, 100, SEARCH_H);
        for (String id : allIds) {
            itemIconCache.put(id, resolveIcon(id));
        }
        applyFilter();
    }


    private static final java.util.Map<String, String> ENTITY_ITEM_ALIASES = java.util.Map.of(
            "eye_of_ender", "ender_eye",
            "wither_skull", "wither_skeleton_skull",
            "dragon_fireball", "fire_charge",
            "small_fireball", "fire_charge",
            "fireball", "fire_charge",
            "spectral_arrow", "spectral_arrow");

    private ItemStack resolveIcon(String idStr) {
        Identifier loc = Identifier.tryParse(idStr);
        if (loc == null)
            return ItemStack.EMPTY;

        var itemOpt = BuiltInRegistries.ITEM.getOptional(loc);
        if (itemOpt.isPresent()) {
            Item item = itemOpt.get();
            if (item != Items.AIR)
                return new ItemStack(item);
        }

        var blockOpt = BuiltInRegistries.BLOCK.getOptional(loc);
        if (blockOpt.isPresent()) {
            Item blockItem = blockOpt.get().asItem();
            if (blockItem != Items.AIR)
                return new ItemStack(blockItem);
        }

        Identifier eggId = Identifier.tryParse(loc.getNamespace() + ":" + loc.getPath() + "_spawn_egg");
        if (eggId != null) {
            var eggOpt = BuiltInRegistries.ITEM.getOptional(eggId);
            if (eggOpt.isPresent()) {
                Item egg = eggOpt.get();
                if (egg != Items.AIR)
                    return new ItemStack(egg);
            }
        }

        String alias = ENTITY_ITEM_ALIASES.get(loc.getPath());
        if (alias != null) {
            Identifier aliasId = Identifier.tryParse(loc.getNamespace() + ":" + alias);
            if (aliasId != null) {
                var aliasOpt = BuiltInRegistries.ITEM.getOptional(aliasId);
                if (aliasOpt.isPresent()) {
                    Item aliasItem = aliasOpt.get();
                    if (aliasItem != Items.AIR) return new ItemStack(aliasItem);
                }
            }
        }

        return ItemStack.EMPTY;
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
        for (String id : allIds) {
            if (!setting.contains(id) && (query.isEmpty() || id.contains(query))) {
                filteredAvailable.add(id);
            }
        }
        if (!query.equals(lastSearchQuery)) {
            targetScrollAvailable = 0;
            lastSearchQuery = query;
        }
    }

    private void recalcLayout() {
        halfW = (WINDOW_W - PANEL_GAP) / 2;
        leftX = winX;
        rightX = winX + halfW + PANEL_GAP;
        searchY = winY + HEADER_H + SEARCH_PADDING;
        // sub-headers sit immediately below the search bar
        int subHeaderY = searchY + SEARCH_H + SEARCH_PADDING;
        // actual list content starts after the sub-headers
        listY = subHeaderY + HEADER_H;
        listH = WINDOW_H - HEADER_H - SEARCH_H - HEADER_H - SEARCH_PADDING * 3;
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        recalcLayout();

        ColorFeature cf = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color primary = cf.getStyledGlobalColor();
        Color textCol = cf.getStyledTextColor(255);
        Color dimText = cf.getStyledTextSecondColor(180);

        panelRenderer.renderPanel(ctx, winX, winY, WINDOW_W, WINDOW_H, HEADER_H);
        panelRenderer.renderHeaderText(ctx, FONT_SERVICE.rendererProvider.getRenderer(),
                "Whitelist – " + setting.getName(), winX, winY, HEADER_H, 6);

        searchBox.setPosition(winX + 4, searchY);
        searchBox.render(ctx, FONT_SERVICE.rendererProvider.getRenderer(), mouseX, mouseY);

        String hint = searchBox.getText().isEmpty() ? "Search..." : "";
        if (!hint.isEmpty()) {
            FONT_SERVICE.drawText(ctx, hint, winX + 8, searchY + (SEARCH_H - 8) / 2 + 1,
                    toRGBA(dimText), false);
        }

        int subHeaderY = searchY + SEARCH_H + SEARCH_PADDING;
        renderSubHeader(ctx, leftX, subHeaderY, halfW, HEADER_H,
                "Available (" + filteredAvailable.size() + ")", primary, textCol);

        List<String> whitelisted = new ArrayList<>(setting.getWhitelist().stream()
                .map(Object::toString).sorted().toList());
        renderSubHeader(ctx, rightX, subHeaderY, halfW, HEADER_H,
                "Whitelisted (" + whitelisted.size() + ")", primary, textCol);

        renderList(ctx, mouseX, mouseY, leftX, listY, halfW, listH,
                filteredAvailable, scrollAvailable, false);
        renderList(ctx, mouseX, mouseY, rightX, listY, halfW, listH,
                whitelisted, scrollWhitelisted, true);

        scrollAvailable += (targetScrollAvailable - scrollAvailable) * 0.25;
        scrollWhitelisted += (targetScrollWhitelisted - scrollWhitelisted) * 0.25;

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void renderSubHeader(GuiGraphics ctx, int x, int y, int w, int h,
            String label, Color bg, Color text) {
        ctx.fill(x, y, x + w, y + h, toRGBA(bg));
        ctx.fill(x, y + h, x + w, y + h + 1, toRGBA(bg.darker()));
        FONT_SERVICE.drawText(ctx, label, x + 4, y + (h - 8) / 2 + 1, toRGBA(text), true);
    }

    private void renderList(GuiGraphics ctx, int mouseX, int mouseY,
            int x, int y, int w, int h,
            List<String> items,
            double scroll,
            boolean isWhitelisted) {

        ColorFeature cf = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color enabledCol = cf.getStyledGlobalColor();
        Color textCol = cf.getStyledTextColor(255);
        Color hoverBg = new Color(enabledCol.getRed(), enabledCol.getGreen(), enabledCol.getBlue(), 40);

        ctx.fill(x, y, x + w, y + h, toRGBA(new Color(20, 20, 20,
                FEATURE_SERVICE.getStorage().getByClass(ClickGuiFeature.class).guiAlpha.get())));

        int maxVisible = h / ROW_H;
        double maxScroll = Math.max(0, items.size() - maxVisible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        if (!isWhitelisted)
            scrollAvailable = scroll;
        else
            scrollWhitelisted = scroll;

        int start = (int) Math.floor(scroll);
        double partial = scroll - start;
        int drawY = y - (int) (partial * ROW_H);

        ScissorUtil.enable(ctx, x, y, x + w, y + h);

        for (int i = start; i < Math.min(items.size(), start + maxVisible + 1); i++) {
            String id = items.get(i);
            boolean hov = mouseX >= x && mouseX < x + w && mouseY >= drawY && mouseY < drawY + ROW_H;

            if (hov) {
                ctx.fill(x, drawY, x + w - SCROLLBAR_W - 1, drawY + ROW_H, toRGBA(hoverBg));
            }

            Color col = isWhitelisted ? enabledCol : textCol;
            if (hov)
                col = Color.WHITE;

            ItemStack icon = itemIconCache.get(id);
            int textOffsetX = 4;
            if (icon != null && !icon.isEmpty()) {
                int iconY = drawY + (ROW_H - ICON_SIZE) / 2;
                ctx.renderItem(icon, x + 2, iconY);
                textOffsetX = 2 + ICON_OFFSET;
            }

            String display = id;
            int availableTextW = w - SCROLLBAR_W - 8 - textOffsetX;
            int maxChars = availableTextW / 5; // rough char width estimate
            if (display.length() > maxChars && maxChars > 4) {
                display = display.substring(0, maxChars - 2) + "..";
            }

            FONT_SERVICE.drawText(ctx, display, x + textOffsetX, drawY + (ROW_H - 8) / 2, toRGBA(col), true);
            drawY += ROW_H;
        }

        ScissorUtil.disable(ctx);

        if (items.size() > maxVisible) {
            int barX = x + w - SCROLLBAR_W - 1;
            ctx.fill(barX, y, barX + SCROLLBAR_W, y + h,
                    toRGBA(new Color(40, 40, 40, 150)));
            float ratio = (float) maxVisible / items.size();
            int thumbH = Math.max((int) (h * ratio), 6);
            int thumbY = y + (int) (scroll / maxScroll * (h - thumbH));
            ctx.fill(barX, thumbY, barX + SCROLLBAR_W, thumbY + thumbH,
                    toRGBA(cf.getStyledGlobalColor()));
        }

        Color border = cf.getStyledSecondColor();
        ctx.fill(x, y, x + w, y + 1, toRGBA(border));
        ctx.fill(x, y + h - 1, x + w, y + h, toRGBA(border));
        ctx.fill(x, y, x + 1, y + h, toRGBA(border));
        ctx.fill(x + w - 1, y, x + w, y + h, toRGBA(border));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        recalcLayout();
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

        if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int barLeftX  = leftX  + halfW - SCROLLBAR_W - 1;
            int barRightX = rightX + halfW - SCROLLBAR_W - 1;

            if (mx >= barLeftX && mx < barLeftX + SCROLLBAR_W && my >= listY && my < listY + listH) {
                draggingScrollLeft = true;
                scrollDragStartY = my;
                scrollDragStartVal = targetScrollAvailable;
                return true;
            }
            if (mx >= barRightX && mx < barRightX + SCROLLBAR_W && my >= listY && my < listY + listH) {
                draggingScrollRight = true;
                scrollDragStartY = my;
                scrollDragStartVal = targetScrollWhitelisted;
                return true;
            }
        }

        List<String> wl = new ArrayList<>(setting.getWhitelist().stream()
                .map(Object::toString).sorted().toList());
        String hitRight = hitTest(mx, my, rightX, listY, halfW, listH, wl, scrollWhitelisted);
        if (hitRight != null) {
            setting.remove(hitRight);
            applyFilter();
            return true;
        }

        String hitLeft = hitTest(mx, my, leftX, listY, halfW, listH, filteredAvailable, scrollAvailable);
        if (hitLeft != null) {
            setting.add(hitLeft);
            applyFilter();
            return true;
        }

        return super.mouseClicked(click, bl);
    }

    private String hitTest(int mx, int my, int x, int y, int w, int h,
            List<String> items, double scroll) {
        if (mx < x || mx >= x + w || my < y || my >= y + h)
            return null;

        int start = (int) Math.floor(scroll);
        double partial = scroll - start;
        int drawY = y - (int) (partial * ROW_H);

        for (int i = start; i < items.size(); i++) {
            if (drawY + ROW_H > y + h)
                break;
            if (my >= drawY && my < drawY + ROW_H)
                return items.get(i);
            drawY += ROW_H;
        }
        return null;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int my = (int) event.y();

        if (draggingScrollLeft) {
            int maxVisible = listH / ROW_H;
            double maxScroll = Math.max(0, filteredAvailable.size() - maxVisible);
            int delta = my - scrollDragStartY;
            double scrollDelta = (listH > 0) ? delta * maxScroll / listH : 0;
            targetScrollAvailable = Math.max(0, Math.min(scrollDragStartVal + scrollDelta, maxScroll));
            return true;
        }

        if (draggingScrollRight) {
            List<String> wl = setting.getWhitelist().stream().map(Object::toString).toList();
            int maxVisible = listH / ROW_H;
            double maxScroll = Math.max(0, wl.size() - maxVisible);
            int delta = my - scrollDragStartY;
            double scrollDelta = (listH > 0) ? delta * maxScroll / listH : 0;
            targetScrollWhitelisted = Math.max(0, Math.min(scrollDragStartVal + scrollDelta, maxScroll));
            return true;
        }

        if (dragging) {
            winX = (int) event.x() - dragOffX;
            winY = (int) event.y() - dragOffY;
            recalcLayout();
            return true;
        } 
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        draggingScrollLeft  = false;
        draggingScrollRight = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        recalcLayout();

        if (mx >= leftX && mx < leftX + halfW && my >= listY && my < listY + listH) {
            int maxVisible = listH / ROW_H;
            double maxScroll = Math.max(0, filteredAvailable.size() - maxVisible);
            targetScrollAvailable = Math.max(0, Math.min(targetScrollAvailable - v * 2, maxScroll));
            return true;
        }

        List<String> wl = setting.getWhitelist().stream().map(Object::toString).toList();
        if (mx >= rightX && mx < rightX + halfW && my >= listY && my < listY + listH) {
            int maxVisible = listH / ROW_H;
            double maxScroll = Math.max(0, wl.size() - maxVisible);
            targetScrollWhitelisted = Math.max(0, Math.min(targetScrollWhitelisted - v * 2, maxScroll));
            return true;
        }

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
