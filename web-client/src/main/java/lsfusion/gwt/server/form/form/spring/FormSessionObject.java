package lsfusion.gwt.form.server.form.spring;

import lsfusion.client.logics.ClientForm;
import lsfusion.interop.form.RemoteFormInterface;

public class FormSessionObject<T> {
    public ClientForm clientForm;
    public RemoteFormInterface remoteForm;
    public String sessionID;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, String sessionID) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.sessionID = sessionID;
    }
}
