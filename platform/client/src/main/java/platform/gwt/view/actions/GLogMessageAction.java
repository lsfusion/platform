package platform.gwt.view.actions;

import java.io.IOException;

public class GLogMessageAction extends GExecuteAction {
    public String message;
    public boolean failed;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GLogMessageAction() {}

    public GLogMessageAction(String message, boolean failed) {
        this.message = message;
        this.failed = failed;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
