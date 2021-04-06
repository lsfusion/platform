package lsfusion.gwt.client.form.property.async;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.object.GObject;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.ExecuteEditContext;

public class GAsyncAddRemove extends GAsyncInputExec {
    public GObject object;
    public Boolean add;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncAddRemove() {
    }

    public GAsyncAddRemove(GObject object, Boolean add) {
        this.object = object;
        this.add = add;
    }

    @Override
    public void exec(GFormController formController, GPropertyDraw property, Event event, ExecuteEditContext editContext, String actionSID) {
        formController.asyncAddRemove(property, editContext, actionSID, this);
    }
}
