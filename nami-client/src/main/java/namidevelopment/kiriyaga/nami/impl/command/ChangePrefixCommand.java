package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import net.minecraft.network.chat.Component;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class ChangePrefixCommand extends Command {

    public ChangePrefixCommand() {
        super("prefix");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {
                new CommandRoute(null, new CommandArgument.StringArg("char", 1, 1))
        };
    }

    @Override
    public void execute(String route, Object[] args) {
        String input = ((String) args[0]).trim();

        COMMAND_SERVICE.getExecutor().setPrefix(input);
        CONFIG_SERVICE.savePrefix(input);

        Component message = CAT_FORMAT.format("{gray}Prefix changed to: {global}" + input + "{gray}.");
        CHAT_SERVICE.sendPersistent(this.getName(), message);
    }
}
