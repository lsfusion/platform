package lsfusion.gwt.form.server;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormSessionManager {
    GForm createForm(String canonicalName, String formSID, RemoteFormInterface remoteForm, Object[] immutableMethods, byte[] firstChanges, LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) throws IOException;

    FormSessionObject getFormSessionObjectOrNull(String formSessionID);

    FormSessionObject getFormSessionObject(String formSessionID);

    FormSessionObject removeFormSessionObject(String formSessionID);
}
