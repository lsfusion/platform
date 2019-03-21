package lsfusion.http.provider.form;

import lsfusion.gwt.client.GForm;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.IOException;

public interface FormProvider {

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException;

    FormSessionObject getFormSessionObject(String formSessionID);
    void removeFormSessionObject(String formSessionID);
    void removeFormSessionObjects(String sessionID);
}
