package namidevelopment.kiriyaga.nami.impl.gui.widget;

public class ActionItem {
    private final String label;
    private final Runnable action;

    public ActionItem(String label, Runnable action) {
        this.label = label;
        this.action = action;
    }

    public String getLabel() { return label; }
    public void execute() { if (action != null) action.run(); }
}
