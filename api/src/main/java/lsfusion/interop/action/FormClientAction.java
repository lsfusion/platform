package lsfusion.interop.action;

import lsfusion.interop.form.ShowFormType;
import lsfusion.interop.form.remote.RemoteFormInterface;

public class FormClientAction extends ExecuteClientAction {

    public String formSID;
    public String canonicalName;
    public RemoteFormInterface remoteForm;
    public ShowFormType showFormType;

    public boolean forbidDuplicate;

    public Object[] immutableMethods;
    public byte[] firstChanges;

    public String formId;

    public static String[] methodNames = new String[] {"getUserPreferences", "getRichDesignByteArray", "getInitFilterPropertyDraw", "getInputObjects"};

    public FormClientAction(String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, ShowFormType showFormType, String formId) {
        this.formSID = formSID;
        this.immutableMethods = immutableMethods;
        this.firstChanges = firstChanges;
        this.canonicalName = canonicalName;
        this.remoteForm = remoteForm;
        this.showFormType = showFormType;
        this.forbidDuplicate = forbidDuplicate;
        this.formId = formId;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "FormClientAction[showFormType: " + showFormType + "]";
    }
}
