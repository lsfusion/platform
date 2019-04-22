package lsfusion.gwt.client.controller.remote.action.form;

public class SetRegularFilter extends FormRequestCountingAction<ServerResponseResult> {
    public int groupId;
    public int filterId;

    public SetRegularFilter() {
    }

    public SetRegularFilter(int groupId, int filterId) {
        this.groupId = groupId;
        this.filterId = filterId;
    }
}
