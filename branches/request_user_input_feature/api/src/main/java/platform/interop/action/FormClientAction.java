package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends ExecuteClientAction {

    public boolean isModal;
    public RemoteFormInterface remoteForm;

    public FormClientAction(boolean isModal, RemoteFormInterface remoteForm) {
        this.isModal = isModal;
        this.remoteForm = remoteForm;
    }

    @Override
    public void execute(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
