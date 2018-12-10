package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.base.shared.actions.NumberResult;

public class CountRecords extends FormRequestIndexCountingAction<NumberResult> {
    public int groupObjectID;

    public CountRecords() {}

    public CountRecords(int groupObjectID) {
        this.groupObjectID = groupObjectID;
    }
}
