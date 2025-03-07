package lsfusion.interop.session;

import lsfusion.base.BaseUtils;
import lsfusion.base.file.FileData;
import lsfusion.base.file.FileStringWithFiles;
import lsfusion.base.file.NamedFileData;
import lsfusion.base.file.StringWithFiles;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;

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

    public static Param getBodyParam(Object value, String charsetName, String name, String fileName) {
        return new Param(value, false, charsetName, name, fileName);
    }

    public static NamedFileData getNamedFile(FileData fileData, String fileName) {
        return new NamedFileData(fileData, BaseUtils.getFileName(fileName != null ? fileName : "file"));
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

    public static class Result implements Serializable {
        public final Object value;

        public final String name; // nullable
        public final String fileName;

        public Result(Object value) {
            this(value, null);
        }

        public Result(Object value, String fileName) {
            this(value, null, fileName);
            assert value instanceof String || value instanceof FileData;
        }

        public Result(Object value, String name, String fileName) {
            this.value = value;
            this.name = name;
            this.fileName = fileName;
        }

        // converting when sending response from the app server to the web server
        public Result convertFileValue(String name, ConvertFileValue valueConverter) {
            Object convertedValue = valueConverter.convertFileValue(value);
            assert convertedValue instanceof String || convertedValue instanceof FileData || convertedValue instanceof StringWithFiles || convertedValue instanceof FileStringWithFiles;
            assert this.name == null;
            return new Result(convertedValue, name, fileName);
        }

        // converting response from the web server to the client
        public Result convertFileValue(ConvertFileValue convertFileValue) {
            assert value instanceof String || value instanceof FileData || value instanceof StringWithFiles || value instanceof StringWithFiles;
            Object convertedValue = convertFileValue.convertFileValue(value);
            assert convertedValue instanceof String || convertedValue instanceof FileData;
            return new Result(convertedValue, name, fileName);
        }

        // converting when sending request from the app server to the client
        public Result convertFileValue(Function<Object, Object> valueConverter, String name) {
            Object convertedValue = valueConverter.apply(value);
            assert convertedValue instanceof String || convertedValue instanceof FileData;
            assert this.name == null;

            String fileName = null;
            if(name != null) {
                // backward compatibility, NAMEDFILE should be used instead
                String[] nameParts = name.split(";");
                if (nameParts.length >= 2) {
                    name = nameParts[0];
                    fileName = nameParts[1];
                }
            }
            return new Result(convertedValue, name, fileName != null ? fileName : this.fileName);
        }
    }
    public static class Param implements Serializable {
        public final Object value; // String or FileData
        public final String charsetName;

        public final boolean url;

        public final String name;
        public final String fileName;

        public Param(Object value, boolean url, String charsetName, String name) {
            this(value, url, charsetName, name, null);
        }
        public Param(Object value, boolean url, String charsetName, String name, String fileName) {
            this.value = value;
            this.url = url;
            this.charsetName = charsetName;
            this.name = name;
            this.fileName = fileName;
        }

        public boolean isImplicitParam() {
            return !url || name.equals(ExternalUtils.PARAMS_PARAM);
        }
    }
}
