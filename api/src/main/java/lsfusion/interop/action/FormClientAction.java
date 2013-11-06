package lsfusion.interop.action;

import lsfusion.interop.ModalityType;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends ExecuteClientAction {

    public String formSID;
    public RemoteFormInterface remoteForm;
    public ModalityType modalityType;

    public FormClientAction(String formSID, RemoteFormInterface remoteForm, ModalityType modalityType) {
        this.formSID = formSID;
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
