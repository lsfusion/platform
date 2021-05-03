package lsfusion.client.form.property.async;

public class ClientInputList {

    public final String[] actions;
    public final ClientAsyncExec[] actionEvents;
    public final boolean strict;

    public ClientInputList(String[] actions, ClientAsyncExec[] actionEvents, boolean strict) {
        this.actions = actions;
        this.actionEvents = actionEvents;
        this.strict = strict;
    }
}
