package platform.gwt.view.actions;

import java.io.IOException;

public class GDialogAction extends GExecuteAction {

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GDialogAction() {}

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
