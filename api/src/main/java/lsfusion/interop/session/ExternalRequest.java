package lsfusion.interop.session;

import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImList;

import java.io.Serializable;
import java.util.Arrays;

public class ExternalRequest implements Serializable {

    public final String[] returnNames;
    public final static String SINGLEBODYPARAMNAME = "body";
    public final String returnMultiType;
    public ExternalRequest(Param[] params) {
        this(params, null, null, null, null, null, null, null, null, null, null);
    }
    public ExternalRequest(Param[] params, String scheme, String method, String webHost,
                           Integer webPort, String contextPath, String servletPath, String pathInfo, String query,
                           String contentType, String sessionId) {
        this(new String[0], params, new String[0], new String[0], null, null, null, null, null, scheme, method, webHost, webPort, contextPath, servletPath, pathInfo, query, contentType, sessionId, null, null, null, false);
    }
    public ExternalRequest(String[] returnNames, Param[] params,
                           String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues,
                           String appHost, Integer appPort, String exportName, String scheme, String method, String webHost,
                           Integer webPort, String contextPath, String servletPath, String pathInfo, String query,
                           String contentType, String sessionId, byte[] body, String signature, String returnMultiType, boolean needNotificationId) {
        this.returnNames = returnNames;
        this.returnMultiType = returnMultiType;
        this.params = params;
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

    public static Param getUrlParam(String value, String charsetName, String name) {
        return new Param(value, true, charsetName, name);
    }

    public static Param getBodyUrlParam(String value, String charsetName, String name) {
        return new Param(value, true, charsetName, name);
    }

    public static Param getBodyParam(Object value, String charsetName, String name) {
        return new Param(value, false, charsetName, name);
    }

    public Param[] params;

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

    public static final ExternalRequest EMPTY = new ExternalRequest(new Param[0]);

    public static Param getSystemParam(String value) {
        return new Param(value, false, ExternalUtils.javaCharset.name(), ExternalUtils.PARAMS_PARAM);
    }

    public Object[] getImplicitParamValues() {
        return Arrays.stream(params)
                .filter(Param::isImplicitParam)
                .map(param -> param.value)
                .toArray();
    }

    public static class Param implements Serializable {
        public final Object value;
        public final boolean url;
        public final String name;
        public final String charsetName;

        public Param(Object value, boolean url, String charsetName, String name) {
            this.value = value;
            this.url = url;
            this.charsetName = charsetName;
            this.name = name;
        }

        public boolean isImplicitParam() {
            return !url || name.equals(ExternalUtils.PARAMS_PARAM);
        }
    }
}
