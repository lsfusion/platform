package lsfusion.interop.session;

import org.apache.hc.core5.http.NameValuePair;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExternalRequest implements Serializable {

    public final String[] returnNames;

    public static class Param implements Serializable {
        public final Object value;
        public final boolean url;
        public final String charsetName;

        public Param(Object value, boolean url, String charsetName) {
            this.value = value;
            this.url = url;
            this.charsetName = charsetName;
        }
    }
    public static Param getUrlParam(String value, String charsetName) {
        return new Param(value, true, charsetName);
    }
    public static Param getBodyUrlParam(String value, String charsetName) {
        return new Param(value, true, charsetName);
    }
    public static Param getBodyParam(Object value, String charsetName) {
        return new Param(value, false, charsetName);
    }
    public static Param getSystemParam(String value) {
        return new Param(value, false, StandardCharsets.UTF_8.toString());
    }
    public Object[] getParamValues() {
        Object[] result = new Object[params.length];
        for(int i = 0; i < params.length; i++)
            result[i] = params[i].value;
        return result;
    }

    public Param[] params;

    public List<NameValuePair> queryParams;
    public String queryParamsCharsetName;

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

    public ExternalRequest(Param[] params) {
        this(new String[0], params, null, null, new String[0], new String[0], null,
                                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, false);
    }

    public ExternalRequest(String[] returnNames, Param[] params, List<NameValuePair> queryParams, String queryParamsCharsetName,
                           String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues,
                           String appHost, Integer appPort, String exportName, String scheme, String method, String webHost,
                           Integer webPort, String contextPath, String servletPath, String pathInfo, String query,
                           String contentType, String sessionId, byte[] body, String signature, boolean needNotificationId) {
        this.returnNames = returnNames;
        this.params = params;
        this.queryParams = queryParams;
        this.queryParamsCharsetName = queryParamsCharsetName;
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
