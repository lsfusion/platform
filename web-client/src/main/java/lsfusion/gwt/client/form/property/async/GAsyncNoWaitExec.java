package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;

import java.util.function.Consumer;

public class GAsyncNoWaitExec extends GAsyncEventExec {
    public GAsyncNoWaitExec() {
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID, Consumer<Long> onExec) {
        onExec.accept(formController.asyncExecutePropertyEventAction(actionSID, editContext, event, null));
    }
}
