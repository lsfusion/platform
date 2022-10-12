package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class GAsyncCloseForm extends GAsyncExec {
    public String canonicalName;

    public GAsyncCloseForm() {
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncCloseForm(editContext, execContext, event, actionSID, pushAsyncResult, externalChange, onExec);
    }

    @Override
    public void exec(FormsController formsController, GFormController formController, FormContainer formContainer, Event editEvent, Supplier<GAsyncFormController> asyncFormController) {
        formController.asyncCloseForm(asyncFormController);
    }
}