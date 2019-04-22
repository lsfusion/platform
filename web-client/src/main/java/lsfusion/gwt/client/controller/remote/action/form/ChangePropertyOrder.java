package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.order.user.GOrder;

public class ChangePropertyOrder extends FormRequestCountingAction<ServerResponseResult> {
    public int propertyID;
    public GGroupObjectValue columnKey;
    public GOrder modiType;

    public ChangePropertyOrder() {}

    public ChangePropertyOrder(int propertyID, GGroupObjectValue columnKey, GOrder modiType) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
        this.modiType = modiType;
    }

}
