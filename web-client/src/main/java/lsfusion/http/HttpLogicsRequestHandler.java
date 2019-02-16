package lsfusion.http;

import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.http.provider.logics.LogicsRunnable;
import lsfusion.http.provider.logics.LogicsSessionObject;
import lsfusion.interop.RemoteLogicsInterface;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public abstract class HttpLogicsRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    protected abstract void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws IOException;

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        runRequest(request, new LogicsRunnable<Object>() {
            @Override
            public Object run(LogicsSessionObject sessionObject) throws IOException {
                handleRequest(sessionObject.remoteLogics, sessionObject.connection, request, response);
                return null;
            }
        });
    }
}
