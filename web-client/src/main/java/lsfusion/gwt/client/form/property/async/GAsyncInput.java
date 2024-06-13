package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GEventSource;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;

import java.util.function.Consumer;

public class GAsyncInput extends GAsyncFormExec {
    public GType changeType;

    public GInputList inputList;
    public GInputListAction[] inputListActions;

    public String customEditFunction;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncInput() {
    }

    public GAsyncInput(GType changeType, GInputList inputList, GInputListAction[] inputListActions, String customEditFunction) {
        this.changeType = changeType;
        this.inputList = inputList;
        this.inputListActions = inputListActions;
        this.customEditFunction = customEditFunction;
    }

    @Override
    public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, GEventSource eventSource, Consumer<Long> onExec) {
        formController.asyncInput(handler, editContext, actionSID, this, eventSource, onExec);
    }
}
