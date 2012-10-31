package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GReportAction extends GExecuteAction {

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GReportAction() {}

    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
