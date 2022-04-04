package lsfusion.client.form.property.async;

import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;

public class ClientInputList {

    public final ClientInputListAction[] actions;
    public final ClientAsyncExec[] actionEvents;
    public final CompletionType completionType;

    public ClientInputList(ClientInputListAction[] actions, ClientAsyncExec[] actionEvents, CompletionType completionType) {
        this.actions = actions;
        this.actionEvents = actionEvents;
        this.completionType = completionType;
    }
}
