package lsfusion.gwt.form.shared.actions.form;

import net.customware.gwt.dispatch.shared.general.StringResult;

public class SingleGroupReport extends FormRequestIndexCountingAction<StringResult> {
    public int groupObjectID;
    public boolean toExcel;

    @SuppressWarnings({"UnusedDeclaration"})
    public SingleGroupReport() {
    }

    public SingleGroupReport(int groupObjectID, boolean toExcel) {
        this.groupObjectID = groupObjectID;
        this.toExcel = toExcel;
    }
}
