package lsfusion.interop.action;

import lsfusion.interop.ModalityType;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends ExecuteClientAction {

    public String formSID;
    public String canonicalName;
    public RemoteFormInterface remoteForm;
    public ModalityType modalityType;

    public Object[] immutableMethods;
    public byte[] firstChanges;

    public static String[] methodNames = new String[] {"getUserPreferences", "getColorPreferences", "getRichDesignByteArray", "getInitFilterPropertyDraw"};

    public FormClientAction(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, ModalityType modalityType) {
        this.formSID = formSID;
        this.immutableMethods = immutableMethods;
        this.firstChanges = firstChanges;
        this.canonicalName = canonicalName;
        this.remoteForm = remoteForm;
        this.modalityType = modalityType;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }

    @Override
    public String toString() {
        return "FormClientAction[modalitType: " + modalityType.name() + "]";
    }
}
