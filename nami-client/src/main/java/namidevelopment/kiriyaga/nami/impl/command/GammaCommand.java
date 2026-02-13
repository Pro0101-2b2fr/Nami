package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.nami.mixininterface.ISimpleOption;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class GammaCommand extends Command {

    public GammaCommand() {
        super("gamma");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null, new CommandArgument.IntArg("value", 0, 420))
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        int newGamma = (int) args[0];

        ((ISimpleOption) (Object) MC.options.gamma()).setValue((double) newGamma);

        CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Gamma set to: {global}" + newGamma + "{gray}."));
    }
}
