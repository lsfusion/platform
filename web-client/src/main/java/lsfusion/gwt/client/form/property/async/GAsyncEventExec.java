package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;

import java.io.Serializable;
import java.util.function.Consumer;

public abstract class GAsyncEventExec implements Serializable {

    public abstract void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec);
}