package platform.interop.action;

import platform.interop.form.RemoteFormInterface;
import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import java.io.IOException;

public class FormClientAction extends ClientAction {

    public boolean isPrintForm;
    public RemoteFormInterface remoteForm;

    public FormClientAction(boolean printForm, RemoteFormInterface remoteForm) {
        isPrintForm = printForm;
        this.remoteForm = remoteForm;
    }

    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return dispatcher.execute(this);
    }
}
