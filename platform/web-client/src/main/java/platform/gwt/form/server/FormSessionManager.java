package platform.gwt.form.server;

import platform.gwt.base.server.LogicsAwareDispatchServlet;
import platform.gwt.form.shared.view.GForm;
import platform.interop.RemoteLogicsInterface;
import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public interface FormSessionManager {
    public GForm createForm(RemoteFormInterface remoteForm, LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) throws IOException;

    public FormSessionObject getFormSessionObject(String formSessionID);

    public FormSessionObject removeFormSessionObject(String formSessionID);
}
