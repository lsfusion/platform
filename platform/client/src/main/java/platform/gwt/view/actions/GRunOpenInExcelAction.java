package platform.gwt.view.actions;

import java.io.IOException;

public class GRunOpenInExcelAction extends GExecuteAction {
    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GRunOpenInExcelAction() {}

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
