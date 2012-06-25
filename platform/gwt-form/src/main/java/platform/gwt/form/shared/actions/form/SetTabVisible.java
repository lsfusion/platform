package platform.gwt.form.shared.actions.form;

public class SetTabVisible extends FormBoundAction<FormChangesResult> {
    public int tabbedPaneID;
    public int tabIndex;

    public SetTabVisible() {
    }

    public SetTabVisible(int tabbedPaneID, int tabIndex) {
        this.tabbedPaneID = tabbedPaneID;
        this.tabIndex = tabIndex;
    }
}
