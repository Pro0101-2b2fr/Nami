package namidevelopment.kiriyaga.api.model.command;

public class CommandRoute {
    private final String literal;
    private final CommandArgument[] arguments;

    public CommandRoute(String literal, CommandArgument... arguments) {
        this.literal = literal;
        this.arguments = arguments;
    }
    public String getLiteral() {
        return literal;
    }

    public CommandArgument[] getArguments() {
        return arguments;
    }
}
