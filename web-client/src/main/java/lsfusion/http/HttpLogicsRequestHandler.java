package lsfusion.http;

import lsfusion.gwt.server.form.logics.LogicsConnection;
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
        runRequest(null, null, null, new Runnable<Object>() {
            @Override
            public Object run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws IOException {
                handleRequest(remoteLogics, logicsConnection, request, response);
                return null;
            }
        });
    }
}
