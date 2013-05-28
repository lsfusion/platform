package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.server.dispatch.SimpleActionHandlerEx;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionManager;
import platform.gwt.form.server.FormSessionObject;
import platform.interop.RemoteLogicsInterface;

public abstract class FormActionHandler<A extends Action<R>, R extends Result> extends SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {
    public FormActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    public FormSessionManager getFormSessionManager() {
        return ((FormDispatchServlet)servlet).getFormSessionManager();

    }

    /**
    * Ищет форму в сессии с name=formSessionID
    *
    * Если форма не найдена, то выбрасывает RuntimeException
    * @throws RuntimeException
    */
    public FormSessionObject getFormSessionObject(String formSessionID) throws RuntimeException {
        return getFormSessionManager().getFormSessionObject(formSessionID);
    }
}
