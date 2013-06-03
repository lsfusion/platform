package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;
import java.io.Serializable;

public class GUpdateEditValueAction extends GExecuteAction {
    public Serializable value;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GUpdateEditValueAction() {}

    public GUpdateEditValueAction(Object value) {
        this.value = (Serializable) value;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
