package lsfusion.gwt.form.server;

import lsfusion.client.logics.ClientForm;
import lsfusion.interop.form.RemoteFormInterface;

public class FormSessionObject<T> {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
    }
}
