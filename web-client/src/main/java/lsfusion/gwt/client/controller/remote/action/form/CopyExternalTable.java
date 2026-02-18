package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public class CopyExternalTable extends FormRequestCountingAction<CopyExternalTableResult> {

    public ArrayList<Integer> propertyIdList;
    public ArrayList<GGroupObjectValue> columnKeys;

    @SuppressWarnings({"UnusedDeclaration"})
    public CopyExternalTable() {
    }

    public CopyExternalTable(ArrayList<Integer> propertyIdList, ArrayList<GGroupObjectValue> columnKeys) {
        this.propertyIdList = propertyIdList;
        this.columnKeys = columnKeys;
    }
}
