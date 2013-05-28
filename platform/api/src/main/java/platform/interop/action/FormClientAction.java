package platform.interop.action;

import platform.interop.ModalityType;
import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends ExecuteClientAction {

    public ModalityType modalityType;
    public RemoteFormInterface remoteForm;

    public FormClientAction(RemoteFormInterface remoteForm, ModalityType modalityType) {
        this.modalityType = modalityType;
        this.remoteForm = remoteForm;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
