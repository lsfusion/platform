package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;

public class GAsyncChange extends GAsyncInputExec {
    public GType changeType;
    public boolean hasList;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncChange() {
    }

    public GAsyncChange(GType changeType, boolean hasList) {
        this.changeType = changeType;
        this.hasList = hasList;
    }

    @Override
    public void exec(GFormController formController, GPropertyDraw property, Event event, ExecuteEditContext editContext, String actionSID) {
        formController.asyncChange(event, editContext, actionSID, this);

    }
}
