package lsfusion.gwt.server.form.provider;

import lsfusion.client.logics.ClientForm;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FormSessionObject<T> {
    public final ClientForm clientForm;
    public final RemoteFormInterface remoteForm;
    public final String sessionID;
    
    public final List<File> savedTempFiles;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, String sessionID) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.sessionID = sessionID;
        
        savedTempFiles = clientForm != null ? new ArrayList<File>() : null;
    }
}
