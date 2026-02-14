package namidevelopment.kiriyaga.nami.impl.feature.hud;

import namidevelopment.kiriyaga.api.model.feature.HudElementFeature;
import namidevelopment.kiriyaga.api.annotation.RegisterFeature;
import namidevelopment.kiriyaga.api.model.setting.BoolSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;

import static namidevelopment.kiriyaga.nami.Nami.*;
import static namidevelopment.kiriyaga.api.NamiApi.*;import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterFeature
public class FacingFeature extends HudElementFeature {

    public final BoolSetting displayLabel = addSetting(new BoolSetting("Label", true));

    public FacingFeature() {
        super("Facing", "Displays player facing direction.", 0, 0, 50, 9);
    }

    @Override
    public Component getDisplayText() {
        Minecraft mc = MC;
        if (mc.player == null) {
            width = FONT_SERVICE.getWidth("NaN");
            height = FONT_SERVICE.getHeight();
            return CAT_FORMAT.format("{global}NaN");
        }

        float yaw = mc.player.getYRot() % 360;
        if (yaw < 0) yaw += 360;

        double rad = Math.toRadians(yaw);
        double dx = -Math.sin(rad);
        double dz = Math.cos(rad);
        double absDx = Math.abs(dx);
        double absDz = Math.abs(dz);

        String dir = switch ((int) Math.floor((yaw + 45) / 90) % 4) {
            case 0 -> "South";
            case 1 -> "West";
            case 2 -> "North";
            case 3 -> "East";
            default -> "Invalid";
        };

        String axisPart;

        if (absDx > 0.2 && absDz > 0.2 && Math.abs(absDx - absDz) < 0.4) {
            String axisX = dx > 0 ? "+X" : "-X";
            String axisZ = dz > 0 ? "+Z" : "-Z";
            axisPart = formatAxis(axisX) + "{secondary}, {global} " + formatAxis(axisZ);
        } else {
            if (absDz > absDx) {
                String axis = dz > 0 ? "+Z" : "-Z";
                axisPart = formatAxis(axis);
            } else {
                String axis = dx > 0 ? "+X" : "-X";
                axisPart = formatAxis(axis);
            }
        }

        String labelPart = displayLabel.get() ? "{global}" + dir + " {secondary}[" + axisPart + "{secondary}]" : axisPart;

        width = FONT_SERVICE.getWidth(labelPart.replace("{global}", "").replace("{white}", "").replace("{secondary}", ""));
        height = FONT_SERVICE.getHeight();

        return CAT_FORMAT.format(labelPart);
    }

    private String formatAxis(String axis) {
        char sign = axis.charAt(0);
        String letter = axis.substring(1);
        return "{white}" + sign + letter;
    }
}