package lsfusion.http.controller;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.http.authentication.LSFLoginUrlAuthenticationEntryPoint;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.base.exception.RemoteInternalException;
import lsfusion.interop.base.exception.RemoteMessageException;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExternalHttpUtils;
import lsfusion.interop.session.ExternalUtils;
import org.apache.hc.core5.http.HttpEntity;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.List;

import static lsfusion.base.BaseUtils.nvl;

public abstract class ExternalRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {

    public ExternalRequestHandler(LogicsProvider logicsProvider) {
        super(logicsProvider);
    }

    protected abstract void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception;

    private void handleRequestException(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response, boolean retry) throws RemoteException {
        try {
            handleRequest(sessionObject, request, response);
        } catch (Exception e) {
            if(e instanceof AuthenticationException) {
                if(((AuthenticationException) e).redirect) {
                    try {
                        request.getSession(true).setAttribute("SPRING_SECURITY_LAST_EXCEPTION", e);
                        LSFLoginUrlAuthenticationEntryPoint.requestCache.saveRequest(request);
                        response.sendRedirect(MainController.getURLPreservingParameters("/login", null, request));
                    } catch (IOException e1) {
                        throw Throwables.propagate(e1);
                    }
                } else {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    writeResponse(response, e.getMessage());
                }
            } else {
                response.setStatus(nvl(e instanceof RemoteInternalException ? ((RemoteInternalException) e).status : null, HttpServletResponse.SC_INTERNAL_SERVER_ERROR));
                response.setContentType(ExternalUtils.getHtmlContentType(ExternalUtils.javaCharset).toString());

                //we know that there will be a re-request, so we do not write in response - we can call getWriter() only once
                if(!(e instanceof NoSuchObjectException) || retry) {
                    writeResponse(response, getErrorMessage(e));
                }

                if (e instanceof RemoteException)  // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    throw (RemoteException) e;
            }
        }
    }

    private void writeResponse(HttpServletResponse response, String message) {
        try { // in theory here can be changed exception (despite of the fact that remote call is wrapped into RemoteExceptionAspect)
            response.getWriter().print(message);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    private String getErrorMessage(Exception e) {
        if(e instanceof RemoteMessageException)
            return e.getMessage();
        
        Pair<String, RemoteInternalException.ExStacks> actualStacks = RemoteInternalException.toString(e);
        return actualStacks.first+'\n'+ ExceptionUtils.getExStackTrace(actualStacks.second.javaStack, actualStacks.second.lsfStack)+'\n'+actualStacks.second.asyncStacks;
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        try {
            runRequest(request, (sessionObject, retry) -> {
                handleRequestException(sessionObject, request, response, retry);
                return null;
            });
        } catch (RemoteException e) { // will suppress that error, because we rethrowed it when handling request (see above)
        }
    }

    // copy of ExternalHTTPServer.sendResponse
    protected void sendResponse(HttpServletResponse response, HttpServletRequest request, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
        if(responseHttpEntity instanceof ExternalUtils.ResultExternalResponse)
            sendResponse(response, (ExternalUtils.ResultExternalResponse) responseHttpEntity);
        else if(responseHttpEntity instanceof ExternalUtils.RedirectExternalResponse) {
            ExternalUtils.RedirectExternalResponse redirect = (ExternalUtils.RedirectExternalResponse) responseHttpEntity;
            response.sendRedirect(MainController.getDirectUrl("/" + redirect.url, REDIRECT_PARAMS, redirect.notification != null ? GwtSharedUtils.NOTIFICATION_PARAM + "=" + redirect.notification : null, request));
        } else {
            response.setContentType("text/html");
            response.getWriter().print(((ExternalUtils.HtmlExternalResponse) responseHttpEntity).html);
        }
    }

    private static final List<String> REDIRECT_PARAMS = BaseUtils.addList(GwtSharedUtils.NOTIFICATION_PARAM, ExternalUtils.PARAMS);

    protected void sendResponse(HttpServletResponse response, ExternalUtils.ResultExternalResponse responseHttpEntity) throws IOException {
        HttpEntity responseEntity = responseHttpEntity.response;
        String contentType = responseEntity.getContentType();
        String contentDisposition = responseHttpEntity.contentDisposition;
        String[] headerNames = responseHttpEntity.headerNames;
        String[] headerValues = responseHttpEntity.headerValues;
        String[] cookieNames = responseHttpEntity.cookieNames;
        String[] cookieValues = responseHttpEntity.cookieValues;
        int statusHttp = responseHttpEntity.statusHttp;

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
                hasContentDisposition = hasContentDisposition || headerName.equals(ExternalUtils.CONTENT_DISPOSITION_HEADER);
            }
        }

        if(cookieNames != null) {
            for (int i = 0; i < cookieNames.length; i++)
                response.addCookie(ExternalHttpUtils.parseCookie(cookieNames[i], cookieValues[i]));
        }

        if(contentType != null && !hasContentType)
            response.setContentType(contentType);
        if(contentDisposition != null && !hasContentDisposition)
            response.addHeader(ExternalUtils.CONTENT_DISPOSITION_HEADER, contentDisposition);
        response.setStatus(statusHttp);
        responseEntity.writeTo(response.getOutputStream());
    }
}
