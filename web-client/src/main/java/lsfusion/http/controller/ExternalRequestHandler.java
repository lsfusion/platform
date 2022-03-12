package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.LogicsSessionObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.StringEntity;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;

import static lsfusion.base.BaseUtils.nvl;

public abstract class ExternalRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    protected abstract void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception;

    private void handleRequestException(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response, boolean retry) throws RemoteException {
        try {
            handleRequest(sessionObject, request, response);
        } catch (Exception e) {
            if(e instanceof AuthenticationException) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/html; charset=utf-8");

                if(!(e instanceof NoSuchObjectException) || retry) {
                    String errString = getErrorMessage(e);
                    try { // in theory here can be changed exception (despite of the fact that remote call is wrapped into RemoteExceptionAspect)
                        response.getWriter().print(errString);
                    } catch (IOException e1) {
                        throw Throwables.propagate(e1);
                    }
                }

                if (e instanceof RemoteException)  // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    throw (RemoteException) e;
            }
        }
    }

    private String getErrorMessage(Exception e) {
        if(e instanceof RemoteMessageException)
            return e.getMessage();
        
        Pair<String, Pair<String, String>> actualStacks = RemoteInternalException.toString(e);
        return actualStacks.first+'\n'+ ExceptionUtils.getExStackTrace(actualStacks.second.first, actualStacks.second.second);
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            runRequest(request, new LogicsRunnable<Object>() {
                @Override
                public Object run(LogicsSessionObject sessionObject, boolean retry) throws RemoteException {
                    handleRequestException(sessionObject, request, response, retry);
                    return null;
                }
            });
        } catch (RemoteException e) { // will suppress that error, because we rethrowed it when handling request (see above)
        }
    }

    protected void sendResponse(HttpServletResponse response, String message, Charset charset, Integer statusHttp) throws IOException {
        sendResponse(response, new ExternalUtils.ExternalResponse(new StringEntity(message, charset), null, null, null, null, null, statusHttp));
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
        Integer statusHttp = nvl(responseHttpEntity.statusHttp, HttpServletResponse.SC_OK);

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
        response.setStatus(statusHttp);
        responseEntity.writeTo(response.getOutputStream());
    }
}
