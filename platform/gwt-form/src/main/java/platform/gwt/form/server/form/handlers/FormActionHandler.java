package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.base.server.handlers.SimpleActionHandlerEx;
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

    /**
     * Ищет форму в сессии с name=sessionId
     *
     * Если форма не найдена, то выбрасывает RuntimeException
     * @throws RuntimeException
     */
    public FormSessionObject getFormSessionObject(String sessionId) throws RuntimeException {
        Object formObj = getSession().getAttribute(sessionId);
        if (formObj instanceof FormSessionObject) {
            return (FormSessionObject) formObj;
        }

        throw new RuntimeException("Форма не найдена.");
    }
}
