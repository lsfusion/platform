package lsfusion.interop.action;

import lsfusion.interop.form.ModalityType;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.util.List;

public class FormClientAction extends ExecuteClientAction {

    public String formSID;
    public String canonicalName;
    public RemoteFormInterface remoteForm;
    public ModalityType modalityType;

    public boolean forbidDuplicate;
    public List<String> inputObjects;

    public Object[] immutableMethods;
    public byte[] firstChanges;

    public static String[] methodNames = new String[] {"getUserPreferences", "getRichDesignByteArray", "getInitFilterPropertyDraw"};

    public FormClientAction(String canonicalName, String formSID, boolean forbidDuplicate, List<String> inputObjects, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, ModalityType modalityType) {
        this.formSID = formSID;
        this.immutableMethods = immutableMethods;
        this.firstChanges = firstChanges;
        this.canonicalName = canonicalName;
        this.remoteForm = remoteForm;
        this.modalityType = modalityType;
        this.forbidDuplicate = forbidDuplicate;
        this.inputObjects = inputObjects;
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
