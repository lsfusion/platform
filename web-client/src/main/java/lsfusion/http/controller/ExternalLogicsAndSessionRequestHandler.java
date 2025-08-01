package lsfusion.http.controller;

import lsfusion.base.Result;
import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.gwt.server.convert.ClientFormChangesToGwtConverter;
import lsfusion.http.authentication.LSFAuthenticationToken;
import lsfusion.http.provider.logics.LogicsProvider;
import lsfusion.http.provider.navigator.NavigatorProviderImpl;
import lsfusion.http.provider.session.SessionProvider;
import lsfusion.interop.connection.ConnectionInfo;
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
import java.util.function.Function;

import static java.util.Collections.list;

public class ExternalLogicsAndSessionRequestHandler extends ExternalRequestHandler {

    public ExternalLogicsAndSessionRequestHandler(LogicsProvider logicsProvider, SessionProvider sessionProvider) {
        super(logicsProvider);
        this.sessionProvider = sessionProvider;
    }

    private final SessionProvider sessionProvider;

    @Override
    protected void handleRequest(LogicsSessionObject sessionObject, HttpServletRequest request, HttpServletResponse response) throws Exception {
        ContentType requestContentType = ExternalUtils.parseContentType(request.getContentType());

        ConnectionInfo connectionInfo = RequestUtils.getConnectionInfo(request);
        RequestUtils.RequestInfo requestInfo = RequestUtils.getRequestInfo(request);

        String logicsHost = sessionObject.connection.host != null && !sessionObject.connection.host.equals("localhost") && !sessionObject.connection.host.equals("127.0.0.1")
                ? sessionObject.connection.host : request.getServerName();

        InputStream requestInputStream = getRequestInputStream(request, requestContentType, requestInfo.query);

        Function<ExternalRequest, ConvertFileValue> convertFileValue = externalRequest -> ClientFormChangesToGwtConverter.getConvertFileValue(sessionObject, request, connectionInfo, externalRequest);

        String paramSessionID = request.getParameter("session");
        Result<String> sessionID = paramSessionID != null ? new Result<>(paramSessionID) : null;
        Result<Boolean> closeSession = new Result<>(false);
        ExecInterface remoteExec = ExternalUtils.getExecInterface(sessionObject.remoteLogics, sessionID, closeSession, sessionProvider.getContainer(), LSFAuthenticationToken.getAppServerToken(), connectionInfo);

        try {
            ExternalUtils.ExternalResponse externalResponse = ExternalUtils.processRequest(remoteExec, convertFileValue, requestInputStream, requestContentType,
                    requestInfo.headerNames, requestInfo.headerValues, requestInfo.cookieNames, requestInfo.cookieValues,
                    logicsHost, sessionObject.connection.port, sessionObject.connection.exportName,
                    request.getScheme(), request.getMethod(), request.getServerName(), request.getServerPort(), request.getContextPath(), request.getServletPath(),
                    requestInfo.pathInfo, requestInfo.query, request.getSession().getId());

            sendResponse(response, request, externalResponse);
        } catch (RemoteException e) {
            closeSession.set(true); // closing session if there is a RemoteException
            throw e;
        } finally {
            if(sessionID != null && closeSession.result) {
                sessionProvider.getContainer().removeSession(sessionID.result);
            }
        }
    }

    // if content type is 'application/x-www-form-urlencoded' the body of the request appears to be already read somewhere else.
    // so we have empty InputStream and have to read body parameters from parameter map
    private InputStream getRequestInputStream(HttpServletRequest request, ContentType contentType, String query) throws IOException {
        InputStream inputStream = request.getInputStream();
        if (contentType != null && ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType.getMimeType()) && inputStream.available() == 0) {
            Charset charset = ExternalUtils.getBodyUrlCharset(contentType);
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
}
