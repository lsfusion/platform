package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.property.GClassViewType;

public class ChangeClassView extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupObjectId;
    public GClassViewType newClassView;

    public ChangeClassView() {}

    public ChangeClassView(int groupObjectId, GClassViewType newClassView) {
        this.groupObjectId = groupObjectId;
        this.newClassView = newClassView;
    }
}
