package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
import platform.gwt.form.server.FormSessionManager;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.interop.RemoteLogicsInterface;

import javax.servlet.http.HttpSession;

public abstract class FormActionHandler<A extends Action<R>, R extends Result> extends SimpleActionHandlerEx<A, R, RemoteLogicsInterface> {
    public FormActionHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    public HttpSession getSession() {
        return servlet.getSession();
    }

    public FormSessionManager getFormSessionManager() {
        return ((RemoteServiceImpl)servlet).getFormSessionManager();

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
