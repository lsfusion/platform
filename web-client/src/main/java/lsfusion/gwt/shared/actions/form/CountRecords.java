package lsfusion.gwt.shared.actions.form;

import lsfusion.gwt.shared.base.result.NumberResult;

public class CountRecords extends FormRequestIndexCountingAction<NumberResult> {
    public int groupObjectID;

    public CountRecords() {}

    public CountRecords(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}
