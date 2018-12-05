package lsfusion.gwt.form.server.form.spring;

import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormProvider {

    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, String sessionID) throws IOException;

    FormSessionObject getFormSessionObjectOrNull(String formSessionID);
    FormSessionObject getFormSessionObject(String formSessionID);
    void removeFormSessionObject(String formSessionID);
    void removeFormSessionObjects(String sessionID);
}
