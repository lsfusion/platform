package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;

import java.util.function.Consumer;

public class GAsyncInput extends GAsyncFormExec {
    public GType changeType;

    public GInputList inputList;

    public String customEditFunction;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncInput() {
    }

    public GAsyncInput(GType changeType, GInputList inputList, String customEditFunction) {
        this.changeType = changeType;
        this.inputList = inputList;
        this.customEditFunction = customEditFunction;
    }

    @Override
    public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncInput(handler, editContext, actionSID, this, onExec);
    }
}
