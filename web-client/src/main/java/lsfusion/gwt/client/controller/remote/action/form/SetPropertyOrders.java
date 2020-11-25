package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.List;

public class SetPropertyOrders extends FormRequestCountingAction<ServerResponseResult> {
    public int groupObjectID;
    public List<Integer> propertyList;
    public List<GGroupObjectValue> columnKeyList;
    public List<Boolean> orderList;

    public SetPropertyOrders() {}

    public SetPropertyOrders(int groupObjectID, List<Integer> propertyList, List<GGroupObjectValue> columnKeyList, List<Boolean> orderList) {
        this.groupObjectID = groupObjectID;
        this.propertyList = propertyList;
        this.columnKeyList = columnKeyList;
        this.orderList = orderList;
    }
}