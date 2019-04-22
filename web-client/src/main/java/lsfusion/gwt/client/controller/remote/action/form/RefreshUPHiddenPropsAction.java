package lsfusion.gwt.client.controller.remote.action.form;

public class RefreshUPHiddenPropsAction extends FormRequestCountingAction<ServerResponseResult> {
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
