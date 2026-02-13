package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class PitchCommand extends Command {

    public PitchCommand() {
        super("pitch");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null, new CommandArgument[] {new CommandArgument.IntArg("value", -90, 90)})
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        int pitch = (int) args[0];

        if (MC.player != null) {
            MC.player.setXRot(pitch);
            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Pitch set to: {global}" + pitch + "{gray}."));
        }
    }
}
