package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class LoadCommand extends Command {

    public LoadCommand() {
        super("load");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null)
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        try {
            CONFIG_SERVICE.loadFeatures();

            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Config has been loaded."));

        } catch (Exception e) {
            CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Config has not been loaded: {global}" + e.getMessage() + "{gray}."));
        }
    }
}
