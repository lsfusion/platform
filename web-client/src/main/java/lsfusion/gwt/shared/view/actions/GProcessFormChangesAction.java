package lsfusion.gwt.shared.view.actions;

import lsfusion.gwt.shared.form.view.dto.GFormChangesDTO;

public class GProcessFormChangesAction extends GExecuteAction {
    public GFormChangesDTO formChanges;

    //needed for it to be gwt-serializable
    @SuppressWarnings("UnusedDeclaration")
    public GProcessFormChangesAction() {}

    public GProcessFormChangesAction(GFormChangesDTO formChanges) {
        this.formChanges = formChanges;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        dispatcher.execute(this);
    }
}
