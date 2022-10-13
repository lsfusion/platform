package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormContainer;

import java.util.function.Consumer;

public class GAsyncCloseForm extends GAsyncExec {
    public String canonicalName;

    public GAsyncCloseForm() {
    }

    @Override
    public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncCloseForm(editContext, execContext, handler, actionSID, pushAsyncResult, externalChange, onExec);
    }

    @Override
    public void exec(FormsController formsController, GFormController formController, FormContainer formContainer, Event editEvent, GAsyncExecutor asyncExecutor) {
        formController.asyncCloseForm(asyncExecutor);
    }

    @Override
    public GPushAsyncResult getPushAsyncResult() {
        return new GPushAsyncClose();
    }
}