package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GLogMessageAction extends GExecuteAction {
    public boolean failed;
    public String message;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GLogMessageAction() {}

    public GLogMessageAction(boolean failed, String message) {
        this.failed = failed;
        this.message = message;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
