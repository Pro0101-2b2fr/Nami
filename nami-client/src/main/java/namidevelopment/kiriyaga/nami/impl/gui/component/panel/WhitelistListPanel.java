package namidevelopment.kiriyaga.nami.impl.gui.component.panel;

import namidevelopment.kiriyaga.api.model.setting.WhitelistSetting;
import namidevelopment.kiriyaga.nami.impl.feature.client.ColorFeature;
import namidevelopment.kiriyaga.nami.impl.gui.base.DataPanel;
import namidevelopment.kiriyaga.nami.impl.gui.entry.WhitelistEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.awt.*;
import java.util.function.Consumer;

import static namidevelopment.kiriyaga.api.NamiApi.FEATURE_SERVICE;
import static namidevelopment.kiriyaga.api.NamiApi.FONT_SERVICE;
import static namidevelopment.kiriyaga.api.util.ColorUtils.toRGBA;

public class WhitelistListPanel extends DataPanel<WhitelistEntry> {

    private final boolean whitelistedSide;
    private final Consumer<WhitelistEntry> onEntryClick;

    public WhitelistListPanel(String name, int x, int y, int width, int height,
            boolean whitelistedSide,
            Consumer<WhitelistEntry> onEntryClick) {
        super(name, x, y, width, height, WhitelistEntry::getDisplayText);
        this.whitelistedSide = whitelistedSide;
        this.onEntryClick = onEntryClick;
        this.inputHeight = 0;
    }

    @Override
    protected void renderEntry(GuiGraphics context, WhitelistEntry item, int x, int y, int w, int h, int mouseX,
            int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;

        ColorFeature cf = FEATURE_SERVICE.getStorage().getByClass(ColorFeature.class);
        Color enabledCol = cf.getStyledGlobalColor();
        Color textCol = cf.getStyledTextColor(255);

        if (hovered) {
            Color hoverBg = new Color(enabledCol.getRed(), enabledCol.getGreen(), enabledCol.getBlue(), 40);
            context.fill(x, y, x + w, y + h, toRGBA(hoverBg));
        }

        Color col = whitelistedSide ? enabledCol : textCol;
        if (hovered)
            col = Color.WHITE;

        ItemStack icon = item.getIcon();
        int textOffsetX = 4;
        if (icon != null && !icon.isEmpty()) {
            int iconSize = 16;
            int iconY = y + (h - iconSize) / 2;
            context.renderItem(icon, x + 2, iconY);
            textOffsetX = 2 + iconSize + 2;
        }

        String display = item.getId();
        int availableTextW = w - 8 - textOffsetX;
        int maxChars = availableTextW / 5;
        if (display.length() > maxChars && maxChars > 4) {
            display = display.substring(0, maxChars - 2) + "..";
        }

        FONT_SERVICE.drawText(context, display, x + textOffsetX, y + (h - 8) / 2, toRGBA(col), true);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            WhitelistEntry entry = getEntryAt(mouseX, mouseY);
            if (entry != null && onEntryClick != null) {
                onEntryClick.accept(entry);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
