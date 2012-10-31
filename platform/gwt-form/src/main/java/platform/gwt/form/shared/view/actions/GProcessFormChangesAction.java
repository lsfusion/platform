package platform.gwt.form.shared.view.actions;

import platform.gwt.form.shared.view.changes.dto.GFormChangesDTO;

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
