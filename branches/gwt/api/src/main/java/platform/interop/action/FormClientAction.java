package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends AbstractClientAction {

    public boolean newSession;
    public boolean isModal;
    public boolean isPrintForm;
    public RemoteFormInterface remoteForm;

    public FormClientAction(boolean isPrintForm, boolean newSession, boolean isModal, RemoteFormInterface remoteForm) {
        this.isPrintForm = isPrintForm;
        this.newSession = newSession;
        this.isModal = isModal;
        this.remoteForm = remoteForm;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
