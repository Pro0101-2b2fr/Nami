package namidevelopment.kiriyaga.api.core.socials;

import namidevelopment.kiriyaga.api.core.config.ConfigService;
import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SocialsService {

    private final ConfigService configService;
    private Map<String, SocialsStatus> socials = new HashMap<>();

    public SocialsService(ConfigService configService) {
        this.configService = configService;
        load();
    }

    public void load() {
        this.socials = new HashMap<>(configService.loadSocials());
    }

    public void setStatus(String name, SocialsStatus status) {
        if (name == null || status == null) return;

        String key = name.toLowerCase();

        if (socials.put(key, status) != status) {
            configService.saveSocials(socials);
        }
    }

    public void remove(String name) {
        if (name == null) return;

        if (socials.remove(name.toLowerCase()) != null) {
            configService.saveSocials(socials);
        }
    }

    public void setStatus(Component name, SocialsStatus status) {
        if (name == null) return;
        setStatus(name.getString(), status);
    }

    public void remove(Component name) {
        if (name == null) return;
        remove(name.getString());
    }

    public boolean isFriend(String name) {
        return getStatus(name) == SocialsStatus.FRIEND;
    }

    public boolean isEnemy(String name) {
        return getStatus(name) == SocialsStatus.ENEMY;
    }

    public SocialsStatus getStatus(String name) {
        if (name == null) return null;
        return socials.get(name.toLowerCase());
    }

    public Map<String, SocialsStatus> getSocials() {
        return Collections.unmodifiableMap(socials);
    }
}
