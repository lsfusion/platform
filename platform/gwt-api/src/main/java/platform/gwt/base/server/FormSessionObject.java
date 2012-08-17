package platform.gwt.base.server;

import platform.client.logics.ClientForm;
import platform.interop.form.RemoteFormInterface;

public class FormSessionObject<T> {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;
    public T userObject;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, T userObject) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.userObject = userObject;
    }
}
