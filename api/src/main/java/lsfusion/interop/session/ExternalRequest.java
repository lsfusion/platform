package lsfusion.interop.session;

import java.io.Serializable;

public class ExternalRequest implements Serializable {

    public final String[] returnNames;
    public Object[] params;

    public final String charsetName;
    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public final String appHost;
    public final Integer appPort;
    public final String exportName;

    public final String scheme;
    public final String method;
    public final String webHost;
    public final Integer webPort;
    public final String contextPath;
    public final String servletPath;
    public final String pathInfo;
    public final String query;
    public final String contentType;
    public final String sessionId;
    public final byte[] body;

    public final String signature;

    public final boolean needNotificationId;

    public ExternalRequest(Object[] params) {
        this(new String[0], params, "utf-8", new String[0], new String[0], null,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
    }

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName,
                           String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues,
                           String appHost, Integer appPort, String exportName, String scheme, String method, String webHost,
                           Integer webPort, String contextPath, String servletPath, String pathInfo, String query,
                           String contentType, String sessionId, byte[] body, String signature, boolean needNotificationId) {
        this.returnNames = returnNames;
        this.params = params;
        this.charsetName = charsetName;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
        this.appHost = appHost;
        this.appPort = appPort;
        this.exportName = exportName;
        this.scheme = scheme;
        this.method = method;
        this.webHost = webHost;
        this.webPort = webPort;
        this.contextPath = contextPath;
        this.servletPath = servletPath;
        this.pathInfo = pathInfo;
        this.query = query;
        this.contentType = contentType;
        this.sessionId = sessionId;
        this.body = body;
        this.signature = signature;
        this.needNotificationId = needNotificationId;
    }
}
