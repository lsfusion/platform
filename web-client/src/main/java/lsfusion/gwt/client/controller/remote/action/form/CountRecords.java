package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.base.result.NumberResult;

public class CountRecords extends FormRequestCountingAction<NumberResult> {
    public int groupObjectID;

    public CountRecords() {}

    public CountRecords(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}