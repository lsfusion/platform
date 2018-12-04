package lsfusion.gwt.form.shared.actions.navigator;

public class GetForm extends NavigatorAction<GetFormResult> {
    public String sid;
    public String canonicalName;
    public boolean isModal;
    public String tabSID;

    public GetForm() {
    }
    
    public GetForm(String canonicalName, String sid, boolean isModal, String tabSID) {
        this.canonicalName = canonicalName;
        this.sid = sid;
        this.isModal = isModal;
        this.tabSID = tabSID;
    }
}
