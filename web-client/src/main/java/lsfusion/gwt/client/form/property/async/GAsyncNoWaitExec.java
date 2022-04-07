package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.function.Consumer;

public class GAsyncNoWaitExec extends GAsyncExec {
    public GAsyncNoWaitExec() {
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncNoWait(editContext, execContext, event, actionSID, this, pushAsyncResult, externalChange, onExec);
    }

    @Override
    public void exec(GAsyncFormController asyncFormController, FormsController formsController, Event editEvent) {
    }
}
