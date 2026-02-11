package namidevelopment.kiriyaga.api.core.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;

import static namidevelopment.kiriyaga.api.NamiApi.*;

public class ConfigDirectoryProvider {

    private final File baseDir;

    public ConfigDirectoryProvider() {
        this.baseDir = new File(FabricLoader.getInstance().getGameDir().toFile(), NAME);
    }

    public File getFeatureConfigDir() {
        return new File(baseDir, "config");
    }

    public File getConfigSaveDir() {
        return new File(baseDir, "configs");
    }

    public File getSocialsFile() {
        return new File(baseDir, "socials.json");
    }

    public File getBaseDir() {
        return baseDir;
    }
}
