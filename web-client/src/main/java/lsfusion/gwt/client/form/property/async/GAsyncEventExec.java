package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;

import java.io.Serializable;
import java.util.function.Consumer;

public abstract class GAsyncEventExec implements Serializable {

    public abstract void exec(GFormController formController, Event event, EditContext editContext, String actionSID, Consumer<Long> onExec);
}