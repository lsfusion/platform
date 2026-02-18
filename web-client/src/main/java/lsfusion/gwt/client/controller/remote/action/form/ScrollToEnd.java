package lsfusion.gwt.client.controller.remote.action.form;


import lsfusion.gwt.client.form.event.GChangeSelection;

public class ScrollToEnd extends FormRequestCountingAction<ServerResponseResult> {
    public int groupId;
    public boolean toEnd;
    public GChangeSelection changeSelection;

    public ScrollToEnd() {
    }

    public ScrollToEnd(int groupId, boolean toEnd, GChangeSelection changeSelection) {
        this.groupId = groupId;
        this.toEnd = toEnd;
        this.changeSelection = changeSelection;
    }
}
