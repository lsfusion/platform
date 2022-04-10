package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
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
    public void exec(GFormController formController, Event event, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncChange(event, editContext, actionSID, this, onExec);
    }
}
