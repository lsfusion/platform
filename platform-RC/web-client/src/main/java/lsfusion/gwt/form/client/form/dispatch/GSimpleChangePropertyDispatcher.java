package lsfusion.gwt.form.client.form.dispatch;

import lsfusion.gwt.form.client.ErrorHandlingCallback;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.GEditBindingMap;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.GUserInputResult;
import lsfusion.gwt.form.shared.view.actions.GRequestUserInputAction;
import lsfusion.gwt.form.shared.view.changes.GGroupObjectValue;


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
