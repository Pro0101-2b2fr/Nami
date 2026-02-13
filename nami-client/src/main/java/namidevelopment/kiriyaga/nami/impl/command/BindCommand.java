package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.api.model.feature.Feature;
import namidevelopment.kiriyaga.api.model.setting.KeyBindSetting;
import namidevelopment.kiriyaga.api.util.KeyUtils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.animal.golem.IronGolem;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class BindCommand extends Command {

    public BindCommand() {
        super("bind");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null, new CommandArgument.FeatureArg("feature"), new CommandArgument.KeyBindArg("key"))
        };
    }

    @Override
    public void execute(String route, Object[] parsedArgs) {
        String featureName = (String) parsedArgs[0];
        String keyName = ((String) parsedArgs[1]).toUpperCase();

        Feature feature = FEATURE_SERVICE.getStorage().getAll().stream().filter(m -> m.getName().equalsIgnoreCase(featureName) || m.matches(featureName)).findFirst().orElse(null);

        if (feature == null) {
            Component message = CAT_FORMAT.format("{gray}Feature {global}" + featureName + " {gray}not found.");
            CHAT_SERVICE.sendPersistent(this.getName(), message);
            return;
        }

        KeyBindSetting bindSetting = feature.getSettings().stream().filter(s -> s instanceof KeyBindSetting).map(s -> (KeyBindSetting) s).findFirst().orElse(null);

        if (bindSetting == null) {
            Component message = CAT_FORMAT.format("{gray}Feature {global}" + featureName + " {gray}does not have a keybind setting.");
            CHAT_SERVICE.sendPersistent(this.getName(), message);
            return;
        }

        int keyCode = KeyUtils.parseKey(keyName);

        if (keyCode == -1) {
            Component message = CAT_FORMAT.format("{gray}Invalid key name: {global}" + keyName + "{gray}.");
            CHAT_SERVICE.sendPersistent(this.getName(), message);
            return;
        }

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyCode);
        if (key == null) {
            Component message = CAT_FORMAT.format("{gray}Invalid key code: {global}" + keyCode + "{gray}.");
            CHAT_SERVICE.sendPersistent(this.getName(), message);
            return;
        }

        bindSetting.set(keyCode);

        Component message = CAT_FORMAT.format("{gray}Bound Feature {global}" + feature.getName() + " {gray}to key {global}" + keyName + "{gray}.");
        CHAT_SERVICE.sendPersistent(this.getName(), message);
    }
}
