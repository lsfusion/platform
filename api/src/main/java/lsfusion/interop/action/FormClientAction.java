package lsfusion.interop.action;

import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.remote.RemoteFormInterface;

public class FormClientAction extends ExecuteClientAction {

    public String formSID;
    public String canonicalName;
    public RemoteFormInterface remoteForm;
    public ModalityType modalityType;

    public boolean forbidDuplicate;

    public Object[] immutableMethods;
    public byte[] firstChanges;

    public static String[] methodNames = new String[] {"getUserPreferences", "getRichDesignByteArray", "getInitFilterPropertyDraw", "getInputObjects"};

    public FormClientAction(String canonicalName, String formSID, boolean forbidDuplicate, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, ModalityType modalityType) {
        this.formSID = formSID;
        this.immutableMethods = immutableMethods;
        this.firstChanges = firstChanges;
        this.canonicalName = canonicalName;
        this.remoteForm = remoteForm;
        this.modalityType = modalityType;
        this.forbidDuplicate = forbidDuplicate;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "FormClientAction[modalitType: " + modalityType.name() + "]";
    }
}
