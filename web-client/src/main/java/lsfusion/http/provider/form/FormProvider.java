package lsfusion.http.provider.form;

import lsfusion.gwt.client.GForm;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.IOException;

public interface FormProvider {

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException;
    void createFormExternal(String formID, RemoteFormInterface remoteForm, String navigatorID);

    FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException;
    Runnable delayedRemoveFormSessionObject(final String formSessionID);
    void removeFormSessionObjects(String sessionID) throws SessionInvalidatedException;
}
