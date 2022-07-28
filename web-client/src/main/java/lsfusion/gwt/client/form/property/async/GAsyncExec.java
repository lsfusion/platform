package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;

public abstract class GAsyncExec extends GAsyncEventExec {

    // without remote call
    public abstract void exec(GAsyncFormController asyncFormController, FormsController formsController, FormContainer formContainer, Event editEvent);

}