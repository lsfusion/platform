package lsfusion.gwt.form.server;

import lsfusion.client.logics.ClientForm;
import lsfusion.interop.form.RemoteFormInterface;

public class FormSessionObject<T> {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;
    public String tabSID;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, String tabSID) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.tabSID = tabSID;
    }
}
