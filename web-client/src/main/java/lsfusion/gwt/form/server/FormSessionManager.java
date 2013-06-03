package lsfusion.gwt.form.server;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormSessionManager {
    public GForm createForm(RemoteFormInterface remoteForm, LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) throws IOException;

    public FormSessionObject getFormSessionObject(String formSessionID);

    public FormSessionObject removeFormSessionObject(String formSessionID);
}
