package namidevelopment.kiriyaga.nami.impl.gui.entry;

import namidevelopment.kiriyaga.api.core.socials.SocialsStatus;
import namidevelopment.kiriyaga.nami.impl.gui.base.BaseEntry;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Collection;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class SocialEntry extends BaseEntry {

    private final String name;
    private SocialsStatus status;
    private boolean online;
    private SocialsStatus lastRenderedStatus = null;

    public SocialEntry(String name, SocialsStatus status) {
        this.name = name;
        this.status = status;
        refreshEntry();
    }

    public SocialEntry(String name) {
        this(name, SocialsStatus.FRIEND);
    }

    public String getName() {
        return name;
    }

    public SocialsStatus getStatus() {
        return status;
    }

    public void setStatus(SocialsStatus status) {
        this.status = status;
        refreshEntry();
    }

    @Override
    public Component getDisplayText() {
        return displayText;
    }

    @Override
    public void refreshEntry() {
        boolean nowOnline = false;

        if (MC.getConnection() != null) {
            Collection<PlayerInfo> list = MC.getConnection().getOnlinePlayers();
            nowOnline = list.stream().anyMatch(entry ->
                    entry.getProfile().name().equalsIgnoreCase(name)
            );
        }

        if (nowOnline != online || displayText == null || lastRenderedStatus != status) {
            online = nowOnline;
            lastRenderedStatus = status;

            String nameColor = switch (status) {
                case FRIEND -> "{friend}";
                case ENEMY -> "{enemy}";
                default -> "{global}";
            };

            displayText = CAT_FORMAT.format(nameColor + name + " {gray}[" + (online ? "{green}Online" : "{red}Offline") + "{gray}]");
        }
    }

}
