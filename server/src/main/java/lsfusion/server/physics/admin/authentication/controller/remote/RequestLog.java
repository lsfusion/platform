package lsfusion.server.physics.admin.authentication.controller.remote;

import lsfusion.server.physics.admin.log.LogInfo;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class RequestLog {
    private final LogInfo logInfo;
    private final String path;
    private final String method;
    private final String requestQuery;
    private final String extraValue;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> requestCookies;
    private final String requestBody;
    private final Map<String, String> responseHeaders;
    private final Map<String, String> responseCookies;
    private final String responseStatus;
    private final String responseExtraValue;
    private final String errorMessage;

    private RequestLog(Builder builder) {
        this.logInfo = builder.logInfo;
        this.path = builder.path;
        this.method = builder.method;
        this.requestQuery = builder.requestQuery;
        this.extraValue = builder.extraValue;
        this.requestHeaders = builder.requestHeaders;
        this.requestCookies = builder.requestCookies;
        this.requestBody = builder.requestBody;
        this.responseHeaders = builder.responseHeaders;
        this.responseCookies = builder.responseCookies;
        this.responseStatus = builder.responseStatus;
        this.responseExtraValue = builder.responseExtraValue;
        this.errorMessage = builder.errorMessage;
    }

    @Override
    public String toString() {
        return "\nREQUEST:\n" +
                (logInfo != null ? "\tREQUEST_USER_INFO: " + logInfo + "\n" : "") +
                (path != null ? "\tREQUEST_PATH: " + path + "\n" : "") +
                (method != null ? "\tREQUEST_METHOD: " + method + "\n" : "") +
                (requestQuery != null ? "\tREQUEST_QUERY: " + requestQuery + "\n" : "") +
                (extraValue != null ? extraValue + "\n" : "") +
                (requestHeaders != null && !requestHeaders.isEmpty()? getLogMapValues("REQUEST_HEADERS:", requestHeaders) + "\n" : "") +
                (requestCookies != null && !requestCookies.isEmpty()? getLogMapValues("REQUEST_COOKIES:", requestCookies) + "\n" : "") +
                (requestBody != null ? "\tBODY:\n\t\t" + requestBody + "\n" : "") +
                "RESPONSE:\n" +
                (responseHeaders != null && !responseHeaders.isEmpty() ? getLogMapValues("RESPONSE_HEADERS:", responseHeaders) + "\n" : "") +
                (responseCookies != null && !responseCookies.isEmpty() ? getLogMapValues("RESPONSE_COOKIES:", responseCookies) + "\n" : "") +
                (responseStatus != null ? "\tRESPONSE_STATUS_HTTP: " + responseStatus + "\n" : "") +
                (responseExtraValue != null ? responseExtraValue : "") +
                (errorMessage != null ? "\tERROR: "  + errorMessage + "\n" : "");
    }

    public static String getLogMapValues(String caption, Map<String, String> map) {
        return "\t" + caption + (map != null ? "\n\t\t" + StringUtils.join(map.entrySet().iterator(), "\n\t\t") : "");
    }

    public static class Builder {
        private LogInfo logInfo = null;
        private String path = null;
        private String method = null;
        private String requestQuery = null;
        private String extraValue = null;
        private Map<String, String> requestHeaders = null;
        private Map<String, String> requestCookies = null;
        private String requestBody = null;
        private Map<String, String> responseHeaders = null;
        private Map<String, String> responseCookies = null;
        private String responseStatus = null;
        private String responseExtraValue = null;
        private String errorMessage = null;

        public Builder() {}

        public Builder logInfo (LogInfo logInfo) {
            this.logInfo = logInfo;
            return this;
        }
        public Builder path (String path) {
            this.path = path;
            return this;
        }
        public Builder method (String method) {
            this.method = method;
            return this;
        }
        public Builder requestQuery (String requestQuery) {
            this.requestQuery = requestQuery;
            return this;
        }
        public Builder extraValue (String extraValue) {
            this.extraValue = extraValue;
            return this;
        }
        public Builder requestHeaders (Map<String, String> requestHeaders) {
            this.requestHeaders = requestHeaders;
            return this;
        }
        public Builder requestCookies (Map<String, String> requestCookies) {
            this.requestCookies = requestCookies;
            return this;
        }
        public Builder requestBody (String requestBody) {
            this.requestBody = requestBody;
            return this;
        }
        public Builder responseHeaders (Map<String, String> responseHeaders) {
            this.responseHeaders = responseHeaders;
            return this;
        }
        public Builder responseCookies (Map<String, String> responseCookies) {
            this.responseCookies = responseCookies;
            return this;
        }
        public Builder responseStatus (String responseStatus) {
            this.responseStatus = responseStatus;
            return this;
        }
        public Builder responseExtraValue (String responseExtraValue) {
            this.responseExtraValue = responseExtraValue;
            return this;
        }

        public Builder errorMessage (String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public RequestLog build() {
            return new RequestLog(this);
        }
    }
}
