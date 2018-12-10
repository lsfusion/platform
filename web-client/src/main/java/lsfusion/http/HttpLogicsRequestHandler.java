package lsfusion.http;

import lsfusion.gwt.server.logics.LogicsConnection;
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
        runRequest(request.getParameter("host"), parseInt(request.getParameter("port")), request.getParameter("exportName"), new Runnable<Object>() {
            @Override
            public Object run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws IOException {
                handleRequest(remoteLogics, logicsConnection, request, response);
                return null;
            }
        });
    }

    private Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.parseInt(value);
        } catch (Exception e) {
            return null;
        }
    }
}
