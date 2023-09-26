package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.logics.LogicsRunnable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

// native interfaces because either we need to use spring, or we can't use gwt
public class LogicsRequestHandler {

    public LogicsRequestHandler(LogicsProvider logicsProvider) {
        this.logicsProvider = logicsProvider;
    }

    private final LogicsProvider logicsProvider;

    protected <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws IOException {
        try {
            return logicsProvider.runRequest(request, runnable);
        } catch (AppServerNotAvailableDispatchException e) {
            throw Throwables.propagate(e);
        }
    }

}
