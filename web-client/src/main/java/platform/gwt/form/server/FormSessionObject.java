package platform.gwt.form.server;

import platform.client.logics.ClientForm;
import platform.interop.form.RemoteFormInterface;

public class FormSessionObject<T> {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
    }
}
