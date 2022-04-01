package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.function.Consumer;

public class GAsyncNoWaitExec extends GAsyncExec {
    public GAsyncNoWaitExec() {
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID, Consumer<Long> onExec) {
        onExec.accept(formController.asyncExecutePropertyEventAction(actionSID, editContext, event, null));
    }

    @Override
    public void exec(GAsyncFormController asyncFormController, FormsController formsController, Event editEvent, EditContext editContext, GFormController formController) {
    }
}
