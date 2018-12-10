package lsfusion.gwt.server.form.provider;

import lsfusion.gwt.shared.form.view.GForm;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormProvider {

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException;

    FormSessionObject getFormSessionObject(String formSessionID);
    void removeFormSessionObject(String formSessionID);
    void removeFormSessionObjects(String sessionID);
}
