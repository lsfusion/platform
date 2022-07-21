package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;

import java.io.Serializable;
import java.util.function.Consumer;

public class GAsyncChange extends GAsyncFormExec {

    public int[] propertyIDs;

    public Serializable value;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncChange() {
    }

    public GAsyncChange(int[] propertyIDs, Serializable value) {
        this.propertyIDs = propertyIDs;
        this.value = value;
    }

    @Override
    public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncChange(editContext, execContext, handler, actionSID, this, pushAsyncResult, externalChange, onExec);
    }
}
