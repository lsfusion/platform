package platform.gwt.view2.actions;

import platform.gwt.view2.changes.dto.GFormChangesDTO;

import java.io.IOException;

public class GProcessFormChangesAction extends GExecuteAction {
    public GFormChangesDTO formChanges;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GProcessFormChangesAction() {}

    public GProcessFormChangesAction(GFormChangesDTO formChanges) {
        this.formChanges = formChanges;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
