package platform.gwt.base.server;

import platform.client.logics.ClientForm;
import platform.interop.form.RemoteFormInterface;

public class FormSessionObject {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
    }
}
