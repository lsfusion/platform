package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;

public class GLogOutAction extends GExecuteAction {
    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
