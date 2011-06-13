package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;
import org.apache.log4j.Logger;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteFormServiceImpl;

import javax.servlet.http.HttpSession;

public abstract class FormServiceActionHandler<A extends Action<R>, R extends Result> extends SimpleActionHandler<A, R> {
    protected final static Logger logger = Logger.getLogger(RemoteFormServiceImpl.class);
    protected final RemoteFormServiceImpl servlet;

    public FormServiceActionHandler(RemoteFormServiceImpl servlet) {
        this.servlet = servlet;
    }

    public HttpSession getSession() {
        return servlet.getSession();
    }

    public FormSessionObject getSessionFormExceptionally(String sessionId) throws RuntimeException {
        Object formObj = getSession().getAttribute(sessionId);
        if (formObj instanceof FormSessionObject) {
            return (FormSessionObject) formObj;
        }

        throw new RuntimeException("Форма не найдена.");
    }
}
