package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;

import java.io.Serializable;

public abstract class GAsyncEventExec implements Serializable {

    public abstract void exec(GFormController formController, GPropertyDraw property, Event event, ExecuteEditContext editContext);
}