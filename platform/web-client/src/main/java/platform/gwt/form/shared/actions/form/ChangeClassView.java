package platform.gwt.form.shared.actions.form;

import platform.gwt.form.shared.view.GClassViewType;

public class ChangeClassView extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GClassViewType newClassView;

    public ChangeClassView() {}

    public ChangeClassView(int groupObjectId, GClassViewType newClassView) {
        this.groupObjectId = groupObjectId;
        this.newClassView = newClassView;
    }
}
