package platform.gwt.form2.shared.actions.form;

import platform.gwt.base.shared.GOrder;
import platform.gwt.form2.shared.view.changes.dto.GGroupObjectValueDTO;

public class ChangePropertyOrder extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int propertyID;
    public GGroupObjectValueDTO columnKey;
    public GOrder modiType;

    public ChangePropertyOrder() {}

    public ChangePropertyOrder(int propertyID, GGroupObjectValueDTO columnKey, GOrder modiType) {
        this.propertyID = propertyID;
        this.columnKey = columnKey;
        this.modiType = modiType;
    }

}
