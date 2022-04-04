package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;

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
    public void exec(GFormController formController, Event event, EditContext editContext, String actionSID, Consumer<Long> onExec) {
        long requestIndex = formController.asyncExecutePropertyEventAction(actionSID, editContext, event, null);

        for(int propertyID : propertyIDs)
            formController.setLoadingValueAt(propertyID, editContext.getFullKey(), value, requestIndex);

        onExec.accept(requestIndex);
    }
}
