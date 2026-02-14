package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.network.chat.Component;

import java.util.Locale;

import static namidevelopment.kiriyaga.api.NamiApi.*;
import static namidevelopment.kiriyaga.api.NamiApi.SERVER_SERVICE;

@RegisterFeature
public class TpsFeature extends HudElementFeature {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public TpsFeature() {
        super("TPS", "Displays server TPS.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        float avgTps = SERVER_SERVICE.getAverageTPS();
        float latestTps = SERVER_SERVICE.getLatestTPS();
        String tpsText = String.format(Locale.US, "%.2f {secondary}[{white}%.2f{secondary}]", avgTps, latestTps); // locale is wild
        String text = displayLabel.get() ? "{global}TPS: {white}" + tpsText : "{white}" + tpsText;

        width = FONT_SERVICE.getWidth(text.replace("{global}", "").replace("{white}", "").replace("{secondary}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(text);
    }
}