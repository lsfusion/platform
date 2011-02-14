package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends AbstractClientAction {

    public boolean isModal;
    public boolean isPrintForm;
    public RemoteFormInterface remoteForm;

    public FormClientAction(RemoteFormInterface remoteForm) {
        this(false, true, remoteForm);
    }

    public FormClientAction(boolean isPrintForm, RemoteFormInterface remoteForm) {
        this(isPrintForm, false, remoteForm);
    }

    public FormClientAction(boolean isPrintForm, boolean isModal, RemoteFormInterface remoteForm) {
        this.isPrintForm = isPrintForm;
        this.remoteForm = remoteForm;
        this.isModal = isModal;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
