package lsfusion.http.provider.form;

import lsfusion.base.Pair;
import lsfusion.client.form.ClientForm;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormSessionObject<T> {
    public final ClientForm clientForm;
    public final RemoteFormInterface remoteForm;
    public final String navigatorID;
    
    public final Map<String, Pair<String, Runnable>> savedTempFiles;

    public int requestIndex = 0;

    public FormSessionObject(ClientForm clientForm, RemoteFormInterface remoteForm, String navigatorID) {
        this.clientForm = clientForm;
        this.remoteForm = remoteForm;
        this.navigatorID = navigatorID;
        
        savedTempFiles = clientForm != null ? new ConcurrentHashMap<>() : null;
    }
}
