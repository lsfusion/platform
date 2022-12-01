package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;

import java.util.ArrayList;

public class PasteExternalTable extends FormRequestCountingAction<ServerResponseResult> {

    public ArrayList<Integer> propertyIdList;
    public ArrayList<GGroupObjectValue> columnKeys;
    public ArrayList<ArrayList<Object>> values;
    public ArrayList<ArrayList<String>> rawValues;

    @SuppressWarnings({"UnusedDeclaration"})
    public PasteExternalTable() {
    }

    public PasteExternalTable(ArrayList<Integer> propertyIdList, ArrayList<GGroupObjectValue> columnKeys, ArrayList<ArrayList<Object>> values, ArrayList<ArrayList<String>> rawValues) {
        this.propertyIdList = propertyIdList;
        this.columnKeys = columnKeys;
        this.values = values;
        this.rawValues = rawValues;
    }
}
