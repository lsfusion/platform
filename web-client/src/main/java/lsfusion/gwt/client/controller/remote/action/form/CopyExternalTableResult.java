package lsfusion.gwt.client.controller.remote.action.form;

import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;

public class CopyExternalTableResult implements Result {
    public ArrayList<ArrayList<Object>> values;
    public ArrayList<ArrayList<String>> rawValues;

    public CopyExternalTableResult() {
    }

    public CopyExternalTableResult(ArrayList<ArrayList<Object>> values, ArrayList<ArrayList<String>> rawValues) {
        this.values = values;
        this.rawValues = rawValues;
    }

    public ArrayList<ArrayList<Object>> getValues() {
        return values;
    }

    public ArrayList<ArrayList<String>> getRawValues() {
        return rawValues;
    }
}
