package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;

public class GHideFormAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GHideFormAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
