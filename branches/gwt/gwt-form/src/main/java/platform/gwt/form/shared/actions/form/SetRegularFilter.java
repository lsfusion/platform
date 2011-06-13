package platform.gwt.form.shared.actions.form;

public class SetRegularFilter extends FormBoundAction<FormChangesResult> {
    public int groupId;
    public int filterId;

    public SetRegularFilter() {
    }

    public SetRegularFilter(int groupId, int filterId) {
        this.groupId = groupId;
        this.filterId = filterId;
    }
}
