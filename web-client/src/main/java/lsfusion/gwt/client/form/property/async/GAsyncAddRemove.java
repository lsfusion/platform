package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;

import java.util.function.Consumer;

public class GAsyncAddRemove extends GAsyncFormExec {
    // we cannot use GObject, since it's converted not only in design (ClientComponentToGwtConverter) but also in ClientActionToGwtConverter
    // so we need sort of DTO, in GFormChangesDTO, but then we have to do remap, and it is not that clear when we should do this, so we'll use primitive type
    public int object;
    public Boolean add;

    @SuppressWarnings("UnusedDeclaration")
    public GAsyncAddRemove() {
    }

    public GAsyncAddRemove(int object, Boolean add) {
        this.object = object;
        this.add = add;
    }

    @Override
    public void exec(GFormController formController, EventHandler handler, EditContext editContext, ExecContext execContext, String actionSID, GPushAsyncInput pushAsyncResult, boolean externalChange, Consumer<Long> onExec) {
        formController.asyncAddRemove(editContext, execContext, handler, actionSID, this, pushAsyncResult, externalChange, onExec);
    }
}
