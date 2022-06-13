package lsfusion.client.form.property.async;

import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.interop.form.property.Compare;

public class ClientInputList {

    public final ClientInputListAction[] actions;
    public final CompletionType completionType;
    public final Compare compare;

    public ClientInputList(ClientInputListAction[] actions, CompletionType completionType, Compare compare) {
        this.actions = actions;
        this.completionType = completionType;
        this.compare = compare;
    }
}
