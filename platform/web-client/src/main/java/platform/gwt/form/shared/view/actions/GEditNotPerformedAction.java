package platform.gwt.form.shared.view.actions;

import java.io.IOException;

public class GEditNotPerformedAction extends GExecuteAction {
    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
