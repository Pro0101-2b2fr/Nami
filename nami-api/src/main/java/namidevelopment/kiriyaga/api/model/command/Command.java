package namidevelopment.kiriyaga.api.model.command;

import com.mojang.brigadier.CommandDispatcher;
import namidevelopment.kiriyaga.api.core.command.BrigadierCommandAdapter;

public abstract class Command {

    private final String name;

    public Command(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract CommandRoute[] getRoutes();

    public abstract void execute(String routeLiteral, Object[] args);

    public boolean matches(String input) {

        String lower = input.toLowerCase();

        if (lower.equals(name)) return true;

        return false;

    }

    public void register(CommandDispatcher<CommandSource> dispatcher) {
        BrigadierCommandAdapter.register(dispatcher, this);
    }
}
