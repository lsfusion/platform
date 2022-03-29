package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

public abstract class GAsyncExec extends GAsyncEventExec {

    public abstract void exec(GAsyncFormController asyncFormController, FormsController formsController, Event editEvent, EditContext editContext, GFormController formController);

}