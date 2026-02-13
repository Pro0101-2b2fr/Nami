package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.core.socials.SocialsStatus;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;
import namidevelopment.kiriyaga.api.model.command.CommandRoute;

import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class SocialsCommand extends Command {

    public SocialsCommand() {
        super("socials");
    }

    @Override
    public CommandRoute[] getRoutes() {
        return new CommandRoute[]{

                new CommandRoute("add", new CommandArgument[]{new CommandArgument.ActionArg("type", "friend", "enemy"), new CommandArgument.FriendNameArg("name", 1, 32)}),
                new CommandRoute("del", new CommandArgument[]{new CommandArgument.FriendNameArg("name", 1, 32)}),
                new CommandRoute("list", new CommandArgument[]{}),
                new CommandRoute("list-type", new CommandArgument[]{new CommandArgument.ActionArg("type", "friend", "enemy")})
        };
    }

    @Override
    public void execute(String route, Object[] args) {

        switch (route) {

            case "add" -> {
                String type = (String) args[0];
                String name = (String) args[1];

                SocialsStatus status =
                        type.equalsIgnoreCase("enemy")
                                ? SocialsStatus.ENEMY
                                : SocialsStatus.FRIEND;

                SOCIALS_SERVICE.setStatus(name, status);

                CHAT_SERVICE.sendPersistent(
                        getName(),
                        CAT_FORMAT.format("{gray}Added {global}" + type + "{gray}: {global}" + name + "{gray}.")
                );
            }

            case "del" -> {
                String name = (String) args[0];

                SOCIALS_SERVICE.remove(name);

                CHAT_SERVICE.sendPersistent(
                        getName(),
                        CAT_FORMAT.format("{gray}Removed: {global}" + name + "{gray}.")
                );
            }

            case "list" -> {
                printList("all");
            }

            case "list-type" -> {
                String type = (String) args[0];
                printList(type);
            }
        }
    }

    private void printList(String type) {
        Map<String, SocialsStatus> socials = SOCIALS_SERVICE.getSocials();

        if (socials.isEmpty()) {
            CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Socials list is empty."));
            return;
        }

        StringBuilder sb = new StringBuilder();

        if (type.equalsIgnoreCase("friend") || type.equalsIgnoreCase("friends")) {
            sb.append("{gray}Friends: {global}");
            appendList(sb, socials, SocialsStatus.FRIEND);
        } else if (type.equalsIgnoreCase("enemy") || type.equalsIgnoreCase("enemies")) {
            sb.append("{gray}Enemies: {global}");
            appendList(sb, socials, SocialsStatus.ENEMY);
        } else {
            sb.append("{gray}Friends: {global}");
            appendList(sb, socials, SocialsStatus.FRIEND);

            sb.append("\n{gray}Enemies: {global}");
            appendList(sb, socials, SocialsStatus.ENEMY);
        }

        CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format(sb.toString()));
    }

    private void appendList(StringBuilder sb, Map<String, SocialsStatus> socials, SocialsStatus filter) {
        int count = 0;

        for (Map.Entry<String, SocialsStatus> entry : socials.entrySet()) {
            if (entry.getValue() != filter) continue;

            if (count > 0) sb.append("{gray},{global} ");
            sb.append(entry.getKey());
            count++;
        }

        if (count == 0) {
            sb.append("{gray}none");
        }
    }
}
