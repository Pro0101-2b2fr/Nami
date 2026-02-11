package namidevelopment.kiriyaga.nami.impl.command;

import namidevelopment.kiriyaga.api.annotation.RegisterCommand;
import namidevelopment.kiriyaga.api.core.socials.SocialsStatus;
import namidevelopment.kiriyaga.api.model.command.Command;
import namidevelopment.kiriyaga.api.model.command.CommandArgument;

import java.util.Map;

import static namidevelopment.kiriyaga.api.NamiApi.*;

@RegisterCommand
public class SocialsCommand extends Command {

    public SocialsCommand() {
        super("socials", new CommandArgument[]{
                new CommandArgument.ActionArg("add/del/list", "add", "del", "list"),
                new CommandArgument.ActionArg("friend/enemy", "friend", "enemy") {
                    @Override
                    public boolean isRequired() {
                        return false;
                    }
                },
                new CommandArgument.FriendNameArg("name", 1, 32) {
                    @Override
                    public boolean isRequired() {
                        return false;
                    }
                }
        });
    }

    @Override
    public void execute(Object[] args) {
        String action = (String) args[0];

        switch (action) {

            case "add" -> {
                String type = args[1] != null ? (String) args[1] : "friend";
                String name = (String) args[2];

                if (name == null) {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Usage: {global}.socials add <friend/enemy> <name>"));
                    return;
                }

                SocialsStatus status = type.equalsIgnoreCase("enemy") ? SocialsStatus.ENEMY : SocialsStatus.FRIEND;

                SOCIALS_SERVICE.setStatus(name, status);

                CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Added {global}" + type + "{gray}: {global}" + name + "{gray}."));
            }

            case "del" -> {
                String name = (String) args[2];

                if (name == null) {
                    name = (String) args[1];
                }

                if (name == null) {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Usage: {global}.socials del <name>"));
                    return;
                }
                SOCIALS_SERVICE.remove(name);
                CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Removed: {global}" + name + "{gray}."));
            }

            case "list" -> {
                String type = args[1] != null ? (String) args[1] : "all";

                Map<String, SocialsStatus> socials = SOCIALS_SERVICE.getSocials();

                if (socials.isEmpty()) {
                    CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format("{gray}Socials list is empty."));
                    return;
                }

                StringBuilder sb = new StringBuilder();

                if (type.equalsIgnoreCase("friend") || type.equalsIgnoreCase("friends")) {
                    sb.append("{gray}Friends: {global}");
                    appendList(sb, socials, SocialsStatus.FRIEND);
                }
                else if (type.equalsIgnoreCase("enemy") || type.equalsIgnoreCase("enemies")) {
                    sb.append("{gray}Enemies: {global}");
                    appendList(sb, socials, SocialsStatus.ENEMY);
                }
                else {
                    sb.append("{gray}Friends: {global}");
                    appendList(sb, socials, SocialsStatus.FRIEND);

                    sb.append("\n{gray}Enemies: {global}");
                    appendList(sb, socials, SocialsStatus.ENEMY);
                }
                CHAT_SERVICE.sendPersistent(getName(), CAT_FORMAT.format(sb.toString()));
            }
        }
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
