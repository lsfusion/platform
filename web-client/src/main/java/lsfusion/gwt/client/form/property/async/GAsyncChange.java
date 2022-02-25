package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;

public class GAsyncChange extends GAsyncInputExec {
    public GType changeType;
    public String customEditFunction;

    public GInputList inputList;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncChange() {
    }

    public GAsyncChange(GType changeType, GInputList inputList, String customEditFunction) {
        this.changeType = changeType;
        this.inputList = inputList;
        this.customEditFunction = customEditFunction;
    }

    @Override
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID) {
        formController.asyncChange(event, editContext, actionSID, this);
    }
}
