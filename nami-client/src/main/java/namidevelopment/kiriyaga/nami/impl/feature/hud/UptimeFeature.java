package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.network.chat.Component;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;
@RegisterFeature
public class UptimeFeature extends HudElementFeature {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public UptimeFeature() {
        super("Uptime", "Displays total time of minecraft run time.", 0, 0, 120, 10);
    }

    @Override
    public Component getDisplayText() {
        long uptimeMillis = System.currentTimeMillis() - START_TIME;
        long totalMinutes = uptimeMillis / 60000;
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        String formatted;

        if (displayLabel.get()) {
            formatted = String.format("{global}Uptime{secondary}({white}%02d{secondary}:{white}%02d{secondary})", hours, minutes);
        } else {
            formatted = String.format("{secondary}({white}%02d{secondary}:{white}%02d{secondary})", hours, minutes);
        }

        width = FONT_SERVICE.getWidth(formatted.replace("{global}", "").replace("{white}", "").replace("{secondary}", ""));
        height = FONT_SERVICE.getHeight();
        return CAT_FORMAT.format(formatted);
    }
}
