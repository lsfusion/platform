package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GOpenUriAction extends GExecuteAction {
    public String uri;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GOpenUriAction() {}

    public GOpenUriAction(String uri) {
        this.uri = uri;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
