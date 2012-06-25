package platform.gwt.view.actions;

import java.io.IOException;

public class GRunPrintReportAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRunPrintReportAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
