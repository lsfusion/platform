package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.http.provider.logics.LogicsSessionObject;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.http.provider.session.SessionSessionObject;
import lsfusion.interop.exception.AuthenticationException;
import lsfusion.interop.exception.RemoteInternalException;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Enumeration;

import static java.util.Collections.list;

public class ExternalLogicsRequestHandler extends ExternalRequestHandler {
    
    @Autowired
    SessionProvider sessionProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws RemoteException {
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
                    sessionSessionObject = sessionProvider.createSession(sessionObject.remoteLogics, request, sessionID);
                remoteExec = sessionSessionObject.remoteSession;
            } else {
                remoteExec = ExternalUtils.getExecInterface(LSFAuthenticationToken.getAppServerToken(),
                        NavigatorProviderImpl.getSessionInfo(request), sessionObject.remoteLogics);
            }

            String logicsHost = sessionObject.connection.host != null && !sessionObject.connection.host.equals("localhost") && !sessionObject.connection.host.equals("127.0.0.1")
                    ? sessionObject.connection.host : request.getServerName();

            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(remoteExec, request.getRequestURL().toString(), 
                    request.getRequestURI(), query, request.getInputStream(), contentType, headerNames, headerValues, cookieNames, cookieValues,
                    logicsHost, sessionObject.connection.port, sessionObject.connection.exportName);

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
