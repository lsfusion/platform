package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public class PasteExternalTable extends FormRequestIndexCountingAction<ServerResponseResult>  {

    public ArrayList<Integer> propertyIdList;
    public ArrayList<GGroupObjectValue> columnKeys;
    public ArrayList<ArrayList<Object>> values;

    @SuppressWarnings({"UnusedDeclaration"})
    public PasteExternalTable() {
    }

    public PasteExternalTable(ArrayList<Integer> propertyIdList, ArrayList<GGroupObjectValue> columnKeys, ArrayList<ArrayList<Object>> values) {
        this.propertyIdList = propertyIdList;
        this.columnKeys = columnKeys;
        this.values = values;
    }
}
