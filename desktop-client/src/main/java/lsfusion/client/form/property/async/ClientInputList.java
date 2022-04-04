package lsfusion.client.form.property.async;

import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;

public class ClientInputList {

    public final ClientInputListAction[] actions;
    public final CompletionType completionType;

    public ClientInputList(ClientInputListAction[] actions, CompletionType completionType) {
        this.actions = actions;
        this.completionType = completionType;
    }
}
