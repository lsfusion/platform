package lsfusion.gwt.form.shared.actions.form;

import lsfusion.gwt.form.shared.view.GClassViewType;

public class ChangeClassView extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GClassViewType newClassView;

    public ChangeClassView() {}

    public ChangeClassView(int groupObjectId, GClassViewType newClassView) {
        this.groupObjectId = groupObjectId;
        this.newClassView = newClassView;
    }
}
