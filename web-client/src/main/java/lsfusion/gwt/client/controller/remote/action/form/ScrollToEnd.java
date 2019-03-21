package lsfusion.gwt.client.controller.remote.action.form;


public class ScrollToEnd extends FormRequestIndexCountingAction<ServerResponseResult> {
    public int groupId;
    public boolean toEnd;

    public ScrollToEnd() {
    }

    public ScrollToEnd(int groupId, boolean toEnd) {
        this.groupId = groupId;
        this.toEnd = toEnd;
    }
}
