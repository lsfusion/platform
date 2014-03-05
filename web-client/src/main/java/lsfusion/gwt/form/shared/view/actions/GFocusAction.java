package lsfusion.gwt.form.shared.view.actions;

import java.io.IOException;

public class GFocusAction extends GExecuteAction {
    public int propertyId;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GFocusAction() {}

    public GFocusAction(int propertyId) {
        this.propertyId = propertyId;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
