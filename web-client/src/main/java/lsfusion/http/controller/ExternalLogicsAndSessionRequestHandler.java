package lsfusion.http.controller;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.http.provider.session.SessionSessionObject;
import lsfusion.interop.logics.LogicsSessionObject;
import lsfusion.interop.session.ExecInterface;
import lsfusion.interop.session.ExternalHttpUtils;
import lsfusion.interop.session.ExternalUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import static java.util.Collections.list;
import static lsfusion.base.ServerMessages.getString;

public class ExternalLogicsAndSessionRequestHandler extends ExternalRequestHandler {
    
    @Autowired
    SessionProvider sessionProvider;

    @Autowired
    private ServletContext servletContext;

    private String defaultServletName;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String sessionID = null;
        boolean closeSession = false;
        try {

            String servletPath = request.getServletPath();

            if (isResourcePath(servletPath)) {
                //call default handler
                RequestDispatcher rd = servletContext.getNamedDispatcher(getDefaulServletName());
                if (rd != null) {
                    rd.forward(request, response);
                }
                return;
            } else if (isPotentialExec(servletPath)) {
                servletPath = "/exec" + servletPath;
            }

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

            InputStream requestInputStream = getRequestInputStream(request, contentType, query);

            ExternalUtils.ExternalResponse responseHttpEntity = ExternalUtils.processRequest(remoteExec, requestInputStream, contentType,
                    headerNames, headerValues, cookieNames, cookieValues, logicsHost, sessionObject.connection.port, sessionObject.connection.exportName,
                    request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(), request.getContextPath(), servletPath,
                    request.getPathInfo() == null ? "" : request.getPathInfo(), query);

            if (responseHttpEntity.response != null) {
                sendResponse(response, responseHttpEntity);
            } else {
                sendResponse(response, getString(request, "executed.successfully"), StandardCharsets.UTF_8, responseHttpEntity.statusHttp);
            }

        } catch (RemoteException e) {
            closeSession = true; // closing session if there is a RemoteException
            throw e;
        } finally {
            if(sessionID != null && closeSession) {
                sessionProvider.removeSessionSessionObject(sessionID);
            }
        }
    }

    //modified from DefaultServletHttpRequestHandler

    /** Default Servlet name used by Tomcat, Jetty, JBoss, and GlassFish */
    private static final String COMMON_DEFAULT_SERVLET_NAME = "default";

    /** Default Servlet name used by Google App Engine */
    private static final String GAE_DEFAULT_SERVLET_NAME = "_ah_default";

    /** Default Servlet name used by Resin */
    private static final String RESIN_DEFAULT_SERVLET_NAME = "resin-file";

    /** Default Servlet name used by WebLogic */
    private static final String WEBLOGIC_DEFAULT_SERVLET_NAME = "FileServlet";

    /** Default Servlet name used by WebSphere */
    private static final String WEBSPHERE_DEFAULT_SERVLET_NAME = "SimpleFileServlet";

    private String getDefaulServletName() {
        if(defaultServletName == null) {
            if (servletContext.getNamedDispatcher(COMMON_DEFAULT_SERVLET_NAME) != null) {
                defaultServletName = COMMON_DEFAULT_SERVLET_NAME;
            } else if (servletContext.getNamedDispatcher(GAE_DEFAULT_SERVLET_NAME) != null) {
                defaultServletName = GAE_DEFAULT_SERVLET_NAME;
            } else if (servletContext.getNamedDispatcher(RESIN_DEFAULT_SERVLET_NAME) != null) {
                defaultServletName = RESIN_DEFAULT_SERVLET_NAME;
            } else if (servletContext.getNamedDispatcher(WEBLOGIC_DEFAULT_SERVLET_NAME) != null) {
                defaultServletName = WEBLOGIC_DEFAULT_SERVLET_NAME;
            } else if (servletContext.getNamedDispatcher(WEBSPHERE_DEFAULT_SERVLET_NAME) != null) {
                defaultServletName = WEBSPHERE_DEFAULT_SERVLET_NAME;
            } else {
                throw new IllegalStateException("Unable to locate the default servlet for serving static content. ");
            }
        }
        return defaultServletName;
    }

    private boolean isResourcePath(String servletPath) {
        return servletPath.endsWith(".js") || servletPath.endsWith(".jsp") || servletPath.endsWith(".map");
    }

    private boolean isPotentialExec(String servletPath) {
        return !servletPath.equals("/exec") && !servletPath.equals("/eval");
    }

    // if content type is 'application/x-www-form-urlencoded' the body of the request appears to be already read somewhere else. 
    // so we have empty InputStream and have to read body parameters from parameter map
    private InputStream getRequestInputStream(HttpServletRequest request, ContentType contentType, String query) throws IOException {
        InputStream inputStream = request.getInputStream();
        if (contentType != null && ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType.getMimeType()) && inputStream.available() == 0) {
            Charset charset = ExternalUtils.getCharsetFromContentType(contentType);
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
