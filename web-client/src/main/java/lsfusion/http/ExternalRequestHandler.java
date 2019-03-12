package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.interop.session.ExternalUtils;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.Pair;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.http.provider.logics.LogicsRunnable;
import lsfusion.http.provider.logics.LogicsSessionObject;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.http.provider.session.SessionSessionObject;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.logics.RemoteLogicsInterface;
import lsfusion.interop.exception.AuthenticationException;
import lsfusion.interop.exception.RemoteInternalException;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;

import static java.util.Collections.list;
import static lsfusion.base.ServerMessages.getString;

public class ExternalRequestHandler extends LogicsRequestHandler implements HttpRequestHandler {
    
    @Autowired
    SessionProvider sessionProvider;

    private void handleRequest(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
        String sessionID = null;
        boolean closeSession = false;
        try {
            String queryString = request.getQueryString();
            String query = queryString != null ? queryString : "";
            String contentTypeString = request.getContentType();
            ContentType contentType = contentTypeString != null ? ContentType.parse(contentTypeString) : null;

            String[] headerNames = list((Enumeration<String>)request.getHeaderNames()).toArray(new String[0]);
            String[] headerValues = getRequestHeaderValues(request, headerNames);

            OrderedMap<String, String> cookiesMap = getRequestCookies(request);
            String[] cookieNames = cookiesMap.keyList().toArray(new String[0]);
            String[] cookieValues = cookiesMap.values().toArray(new String[0]);

            sessionID = request.getParameter("session");
            ExecInterface remoteExec;
            if(sessionID != null) {
                if(sessionID.endsWith("_close")) {
                    closeSession = true;
                    sessionID = sessionID.substring(0, sessionID.length() - "_close".length());
                }

                SessionSessionObject sessionSessionObject = sessionProvider.getSessionSessionObject(sessionID);
                if(sessionSessionObject == null)
                    sessionSessionObject = sessionProvider.createSession(remoteLogics, request, sessionID);
                remoteExec = sessionSessionObject.remoteSession;
            } else {
                remoteExec = ExternalUtils.getExecInterface(LSFAuthenticationToken.getAppServerToken(),
                        NavigatorProviderImpl.getSessionInfo(request), remoteLogics);
            }

            String logicsHost = logicsConnection.host != null && !logicsConnection.host.equals("localhost") && !logicsConnection.host.equals("127.0.0.1")
                    ? logicsConnection.host : request.getServerName();

            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(remoteExec, request.getRequestURL().toString(), 
                    request.getRequestURI(), query, request.getInputStream(), contentType, headerNames, headerValues, cookieNames, cookieValues,
                    logicsHost, logicsConnection.port, logicsConnection.exportName);

            if (responseHttpEntity.response != null) {
                sendResponse(response, responseHttpEntity);
            } else {
                sendOKResponse(request, response);
            }

        } catch (Exception e) {
            if(e instanceof AuthenticationException) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/html; charset=utf-8");
                try { // in theory here can be changed exception (despite the fact that remote call is wrapped into RemoteExceptionAspect)
                    Pair<String, Pair<String, String>> actualStacks = RemoteInternalException.toString(e);
                    response.getWriter().print(actualStacks.first+'\n'+ ExceptionUtils.getExStackTrace(actualStacks.second.first, actualStacks.second.second));
                } catch (IOException e1) {
                    throw Throwables.propagate(e1);
                }

                if (e instanceof RemoteException) { // rethrow RemoteException to invalidate LogicsSessionObject in LogicsProvider
                    closeSession = true; // closing session if there is a RemoteException
                    throw (RemoteException) e;
                }
            }
        } finally {
            if(sessionID != null && closeSession) {
                sessionProvider.removeSessionSessionObject(sessionID);
            }
        }
    }

    @Override
    public void handleRequest(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException {
        try {
            runRequest(request, new LogicsRunnable<Object>() {
                @Override
                public Object run(LogicsSessionObject sessionObject) throws RemoteException {
                    handleRequest(sessionObject.remoteLogics, sessionObject.connection, request, response);
                    return null;
                }
            });
        } catch (RemoteException e) { // will suppress that error, because we rethrowed it when handling request (see above)
        }
    }

    private void sendOKResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().print(getString(request, "executed.successfully"));
    }

    // copy of ExternalHTTPServer.sendResponse
    private void sendResponse(HttpServletResponse response, ExternalUtils.ExternalResponse responseHttpEntity) throws IOException {
        HttpEntity responseEntity = responseHttpEntity.response;
        Header contentType = responseEntity.getContentType();
        String contentDisposition = responseHttpEntity.contentDisposition;
        String[] headerNames = responseHttpEntity.headerNames;
        String[] headerValues = responseHttpEntity.headerValues;
        String[] cookieNames = responseHttpEntity.cookieNames;
        String[] cookieValues = responseHttpEntity.cookieValues;

        boolean hasContentType = false; 
        boolean hasContentDisposition = false; 
        for(int i=0;i<headerNames.length;i++) {
            String headerName = headerNames[i];
            if(headerName.equals("Content-Type")) {
                hasContentType = true;
                response.setContentType(headerValues[i]);
            } else                
                response.addHeader(headerName, headerValues[i]);
            hasContentDisposition = hasContentDisposition || headerName.equals("Content-Disposition");            
        }

        for (int i = 0; i < cookieNames.length; i++) {
            response.addCookie(new Cookie(cookieNames[i], cookieValues[i]));
        }

        if(contentType != null && !hasContentType)
            response.setContentType(contentType.getValue());
        if(contentDisposition != null && !hasContentDisposition)
            response.addHeader("Content-Disposition", contentDisposition);        
        response.setStatus(HttpServletResponse.SC_OK);
        responseEntity.writeTo(response.getOutputStream());
    }

    private String[] getRequestHeaderValues(HttpServletRequest request, String[] headerNames) {
        String[] headerValuesArray = new String[headerNames.length];
        for (int i = 0; i < headerNames.length; i++) {
            headerValuesArray[i] = StringUtils.join(list(request.getHeaders(headerNames[i])), ",");
        }
        return headerValuesArray;
    }

    private OrderedMap<String, String> getRequestCookies(HttpServletRequest request) {
        OrderedMap<String, String> cookiesMap = new OrderedMap<>();
        String cookies = request.getHeader("Cookie");
        if (cookies != null) {
            for (String cookie : cookies.split(";")) {
                String[] splittedCookie = cookie.split("=");
                if (splittedCookie.length == 2) {
                    cookiesMap.put(splittedCookie[0], splittedCookie[1]);
                }
            }
        }
        return cookiesMap;
    }
}
