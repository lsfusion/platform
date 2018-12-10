package lsfusion.gwt.shared.form.actions.form;

public class RefreshUPHiddenPropsAction extends FormRequestIndexCountingAction<ServerResponseResult> {
    public String groupObjectSID;
    public String[] propSids;

    public RefreshUPHiddenPropsAction() {
        this("", new String[0]);
    }

    public RefreshUPHiddenPropsAction(String groupObjectSID, String[] propSids) {
        this.groupObjectSID = groupObjectSID;
        this.propSids = propSids;
    }
}
