package namidevelopment.kiriyaga.api.core.config;

import com.google.gson.*;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import namidevelopment.kiriyaga.api.core.socials.SocialsStatus;

import static namidevelopment.kiriyaga.api.NamiApi.LOGGER;

public class SocialsStorage {

    private final ConfigDirectoryProvider dirs;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public SocialsStorage(ConfigDirectoryProvider dirs) {
        this.dirs = dirs;
    }

    public void save(Map<String, SocialsStatus> socials) {
        JsonObject obj = new JsonObject();
        for (Map.Entry<String, SocialsStatus> entry : socials.entrySet()) {
            obj.addProperty(entry.getKey().toLowerCase(), entry.getValue().name());
        }

        File file = dirs.getSocialsFile();
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(obj, writer);
        } catch (Exception e) {
            LOGGER.error("Failed to save socials.json", e);
        }
    }

    public Map<String, SocialsStatus> load() {
        Map<String, SocialsStatus> socials = new HashMap<>();
        File file = dirs.getSocialsFile();
        if (!file.exists()) return socials;

        try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);

            if (element.isJsonObject()) {
                JsonObject obj = element.getAsJsonObject();

                for (String key : obj.keySet()) {
                    JsonElement val = obj.get(key);

                    if (val != null && val.isJsonPrimitive()) {
                        try {
                            SocialsStatus status = SocialsStatus.valueOf(val.getAsString().toUpperCase());
                            socials.put(key.toLowerCase(), status);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load socials.json", e);
        }

        return socials;
    }
}
