package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.core.macro.model.Macro;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;
import namidevelopment.kiriyaga.api.util.KeyUtils;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class MacroCommand extends Command {

    public MacroCommand() {
        super("macro");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[] {

                new CommandRoute("add", new CommandArgument[] {new CommandArgument.KeyBindArg("key"), new CommandArgument.StringArg("message", 1, 256)}),

                new CommandRoute("del", new CommandArgument[] {new CommandArgument.KeyBindArg("key")}),

                new CommandRoute("list", new CommandArgument[0])
        };
    }

    @Override
    public void execute(String route, Object[] args) {

        switch (route) {

            case "add" -> {
                String keyName = ((String) args[0]).toUpperCase();
                String message = (String) args[1];

                int keyCode = KeyUtils.parseKey(keyName);
                if (keyCode == -1) {
                    CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Invalid key: {global}" + keyName + "{gray}."));
                    return;
                }

                MACRO_SERVICE.addMacro(new Macro(keyCode, message));
                CONFIG_SERVICE.saveMacros();

                CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Macro added: {global}" + keyName + " " + message + "{gray}."));
            }

            case "del" -> {
                String keyName = ((String) args[0]).toUpperCase();

                int keyCode = KeyUtils.parseKey(keyName);
                if (keyCode == -1) {
                    CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Invalid key: {global}" + keyName + "{gray}."));
                    return;
                }

                MACRO_SERVICE.removeMacro(keyCode);
                CONFIG_SERVICE.saveMacros();

                CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}Macro removed: {global}" + keyName + "{gray}."));
            }

            case "list" -> {
                if (MACRO_SERVICE.getAll().isEmpty()) {
                    CHAT_SERVICE.sendPersistent(this.getName(), CAT_FORMAT.format("{gray}No macros have been added."));
                    return;
                }

                StringBuilder builder = new StringBuilder();
                builder.append("{gray}Macros:\n");

                for (Macro macro : MACRO_SERVICE.getAll()) {
                    String key = KeyUtils.getKeyName(macro.getKeyCode());
                    String msg = macro.getMessage();

                    builder.append("  {global}")
                            .append(key)
                            .append(" ")
                            .append(msg)
                            .append("{gray}\n");
                }

                CHAT_SERVICE.sendPersistent(
                        this.getName(),
                        CAT_FORMAT.format(builder.toString())
                );
            }

            default -> {
                CHAT_SERVICE.sendPersistent(this.getName(),
                        CAT_FORMAT.format("{gray}Unknown route: {global}" + route + "{gray}."));
            }
        }
    }
}
