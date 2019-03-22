package lsfusion.http.controller;

import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;

import static lsfusion.base.ServerMessages.getString;

public class ExternalRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            runRequest(request, new LogicsRunnable<Object>() {
                @Override
                public Object run(LogicsSessionObject sessionObject) throws RemoteException {
                    handleRequest(sessionObject, request, response);
                    return null;
                }
            });
        } catch (RemoteException e) { // will suppress that error, because we rethrowed it when handling request (see above)
        }
    }

    // copy of ExternalHTTPServer.sendResponse
    protected void sendResponse(HttpServletResponse response, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
        HttpEntity responseEntity = responseHttpEntity.response;
        Header contentType = responseEntity.getContentType();
        String contentDisposition = responseHttpEntity.contentDisposition;
        String[] headerNames = responseHttpEntity.headerNames;
        String[] headerValues = responseHttpEntity.headerValues;
        String[] cookieNames = responseHttpEntity.cookieNames;
        String[] cookieValues = responseHttpEntity.cookieValues;

        boolean hasContentType = false; 
        boolean hasContentDisposition = false;
        if(headerNames != null) {
            for (int i = 0; i < headerNames.length; i++) {
                String headerName = headerNames[i];
                if (headerName.equals("Content-Type")) {
                    hasContentType = true;
                    response.setContentType(headerValues[i]);
                } else {
                    response.addHeader(headerName, headerValues[i]);
                }
                hasContentDisposition = hasContentDisposition || headerName.equals("Content-Disposition");
            }
        }

        if(cookieNames != null) {
            for (int i = 0; i < cookieNames.length; i++) {
                response.addCookie(new Cookie(cookieNames[i], cookieValues[i]));
            }
        }

        if(contentType != null && !hasContentType)
            response.setContentType(contentType.getValue());
        if(contentDisposition != null && !hasContentDisposition)
            response.addHeader("Content-Disposition", contentDisposition);        
        response.setStatus(HttpServletResponse.SC_OK);
        responseEntity.writeTo(response.getOutputStream());
    }

    protected void sendOKResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        sendResponse(response, getString(request, "executed.successfully"), false);
    }

    protected void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        sendResponse(response, message, true);
    }

    protected void sendResponse(HttpServletResponse response, String message, boolean error) throws IOException {
        response.setStatus(error ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : HttpServletResponse.SC_OK);
        response.getWriter().print(message);
    }
}
