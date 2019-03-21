package lsfusion.gwt.client.form.property.cell.controller.dispatch;

import lsfusion.gwt.client.base.exception.ErrorHandlingCallback;
import lsfusion.gwt.client.form.controller.dispatch.GFormActionDispatcher;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.shared.form.property.cell.GEditBindingMap;
import lsfusion.gwt.shared.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.shared.action.GRequestUserInputAction;
import lsfusion.gwt.shared.form.object.GGroupObjectValue;


public class GSimpleChangePropertyDispatcher extends GFormActionDispatcher {
    private Object value = null;

    public GSimpleChangePropertyDispatcher(GFormController form) {
        super(form);
    }

    public boolean changeProperty(Object value, GPropertyDraw property, GGroupObjectValue columnKey, boolean isChangeWYS) {
        this.value = value;
        form.executeEditAction(property, columnKey, isChangeWYS ? GEditBindingMap.CHANGE_WYS : GEditBindingMap.CHANGE, new ErrorHandlingCallback<ServerResponseResult>() {
            @Override
            public void success(ServerResponseResult result) {
                dispatchResponse(result);
            }
        });
        return true;
    }

    @Override
    public Object execute(GRequestUserInputAction action) {
        return new GUserInputResult(value);
    }
}
