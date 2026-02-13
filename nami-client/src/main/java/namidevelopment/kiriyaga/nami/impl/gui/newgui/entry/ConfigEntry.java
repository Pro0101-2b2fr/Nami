package namidevelopment.kiriyaga.nami.impl.gui.newgui.entry;

import namidevelopment.kiriyaga.api.core.config.model.ConfigMeta;
import namidevelopment.kiriyaga.nami.impl.gui.newgui.base.BaseEntry;
import net.minecraft.network.chat.Component;

import static namidevelopment.kiriyaga.api.NamiApi.CAT_FORMAT;

public class ConfigEntry extends BaseEntry {

    private final String name;
    private final ConfigMeta meta;

    public ConfigEntry(String name, ConfigMeta meta) {
        this.name = name;
        this.meta = meta;

        if (meta == null) {
            this.displayText = CAT_FORMAT.format("{gray}Name: {global}"+name + " {gray}Saved: {global}unknown" + "{gray}Author: {global}unknown" + "{gray}Version: {global}unknown");
        } else {
            this.displayText = CAT_FORMAT.format("{gray}Name: {global}"+name + " {gray}Saved: {global}" + meta.mode().name() +" {gray}Author: {global}" + meta.author() + " {gray}Version: {global}" + meta.clientVersion());
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public Component getDisplayText() {
        return this.displayText;
    }

    @Override
    public void refreshEntry() {}
}
