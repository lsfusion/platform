package lsfusion.http.controller;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.base.file.StringWithFiles;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.http.provider.session.SessionSessionObject;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.*;
import org.apache.commons.lang.StringUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import static java.util.Collections.list;

public class ExternalLogicsAndSessionRequestHandler extends ExternalRequestHandler {

    public ExternalLogicsAndSessionRequestHandler(LogicsProvider logicsProvider, SessionProvider sessionProvider, ServletContext servletContext) {
        super(logicsProvider);
        this.sessionProvider = sessionProvider;
        this.servletContext = servletContext;
    }

    private final SessionProvider sessionProvider;
    private final ServletContext servletContext;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionID = null;
        boolean closeSession = false;
        try {
            String queryString = request.getQueryString();
            String query = queryString != null ? queryString : "";
            ContentType requestContentType = ExternalUtils.parseContentType(request.getContentType());

            SessionInfo sessionInfo = NavigatorProviderImpl.getSessionInfo(request);

            String[] headerNames = list(request.getHeaderNames()).toArray(new String[0]);
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
            } else
                remoteExec = ExternalUtils.getExecInterface(LSFAuthenticationToken.getAppServerToken(), sessionInfo, sessionObject.remoteLogics);

            String logicsHost = sessionObject.connection.host != null && !sessionObject.connection.host.equals("localhost") && !sessionObject.connection.host.equals("127.0.0.1")
                    ? sessionObject.connection.host : request.getServerName();

            InputStream requestInputStream = getRequestInputStream(request, requestContentType, query);

            ConvertFileValue convertFileValue = value -> {
                if(value instanceof StringWithFiles) {
                    StringWithFiles stringWithFiles = (StringWithFiles) value;
                    return ExternalUtils.convertFileValue(stringWithFiles.prefixes, ClientFormChangesToGwtConverter.convertFileValue(stringWithFiles.files, servletContext, sessionObject, sessionInfo));
                }
                return value;
            };

            ExternalUtils.ExternalResponse externalResponse = ExternalUtils.processRequest(remoteExec, convertFileValue, requestInputStream, requestContentType,
                    headerNames, headerValues, cookieNames, cookieValues, logicsHost, sessionObject.connection.port, sessionObject.connection.exportName,
                    request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(), request.getContextPath(), request.getServletPath(),
                    request.getPathInfo() == null ? "" : request.getPathInfo(), query, request.getSession().getId());

            sendResponse(response, request, externalResponse);
        } catch (RemoteException e) {
            closeSession = true; // closing session if there is a RemoteException
            throw e;
        } finally {
            if(sessionID != null && closeSession) {
                sessionProvider.removeSessionSessionObject(sessionID);
            }
        }
    }
    
    // if content type is 'application/x-www-form-urlencoded' the body of the request appears to be already read somewhere else. 
    // so we have empty InputStream and have to read body parameters from parameter map
    private InputStream getRequestInputStream(HttpServletRequest request, ContentType contentType, String query) throws IOException {
        InputStream inputStream = request.getInputStream();
        if (contentType != null && ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType.getMimeType()) && inputStream.available() == 0) {
            Charset charset = ExternalUtils.getCharsetFromContentType(contentType, true);
            List<NameValuePair> queryParams = URLEncodedUtils.parse(query, charset);
            StringBuilder bodyParams = new StringBuilder();

            Map parameterMap = request.getParameterMap();
            for (Object o : parameterMap.entrySet()) {
                Object paramName = ((Map.Entry) o).getKey();
                Object paramValues = ((Map.Entry) o).getValue();

                if (paramName instanceof String && paramValues instanceof String[]) {
                    for (String paramValue : (String[]) paramValues) {
                        NameValuePair parameter = new BasicNameValuePair((String) paramName, paramValue);
                        if (!queryParams.contains(parameter)) {
                            if (bodyParams.length() > 0) {
                                bodyParams.append("&");
                            }
                            // params in inputStream should be URL encoded. parameterMap contains decoded params
                            String encodedParamValue = URLEncoder.encode(paramValue, charset.name());
                            bodyParams.append((String) paramName).append("=").append(encodedParamValue);
                        }
                    }
                }
            }

            inputStream = new ByteArrayInputStream(bodyParams.toString().getBytes(charset));
        }
        return inputStream;
    }

    private String[] getRequestHeaderValues(HttpServletRequest request, String[] headerNames) {
        String[] headerValuesArray = new String[headerNames.length];
        for (int i = 0; i < headerNames.length; i++) {
            headerValuesArray[i] = StringUtils.join(list(request.getHeaders(headerNames[i])).iterator(), ",");
        }
        return headerValuesArray;
    }

    public static OrderedMap<String, String> getRequestCookies(HttpServletRequest request) {
        OrderedMap<String, String> cookiesMap = new OrderedMap<>();
        Cookie[] cookies = request.getCookies();
        if (cookies != null)
            for (Cookie cookie : cookies)
                ExternalHttpUtils.formatCookie(cookiesMap, cookie);
        return cookiesMap;
    }
}
