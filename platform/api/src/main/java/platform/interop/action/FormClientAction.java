package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class FormClientAction extends AbstractClientAction {

    public boolean isPrintForm;
    public RemoteFormInterface remoteForm;

    public FormClientAction(boolean printForm, RemoteFormInterface remoteForm) {
        isPrintForm = printForm;
        this.remoteForm = remoteForm;
    }

    @Override
    public void dispatch(ClientActionDispatcher dispatcher) throws IOException {
        dispatcher.execute(this);
    }
}
