package lsfusion.http.provider.form;

import lsfusion.gwt.client.GForm;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.io.IOException;

public interface FormProvider {

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException;
    String getFile(String navigatorID, String fileName) throws SessionInvalidatedException;
    void createFormExternal(String formID, RemoteFormInterface remoteForm, String navigatorID);

    FormSessionObject getFormSessionObject(String formSessionID) throws SessionInvalidatedException;
    void scheduleRemoveFormSessionObject(final String formSessionID, long delay);
    void removeFormSessionObjects(String sessionID) throws SessionInvalidatedException;
}
