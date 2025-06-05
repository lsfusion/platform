package lsfusion.server.physics.admin.authentication.controller.remote;

import lsfusion.server.physics.admin.log.LogInfo;

import java.util.Map;

public class RequestLog {
    private final boolean detailLog;
    private final LogInfo logInfo;
    private final String path;
    private final String method;
    private final String extraValue;
    private final Map<String, String> requestHeaders;
    private final Map<String, String> requestCookies;
    private final String requestBody;
    private final String requestExtraValue;
    private final Map<String, String> responseHeaders;
    private final Map<String, String> responseCookies;
    private final String responseStatus;
    private final String responseExtraValue;
    private final String errorMessage;

    private RequestLog(Builder builder) {
        this.detailLog = builder.detailLog;
        this.logInfo = builder.logInfo;
        this.path = builder.path;
        this.method = builder.method;
        this.extraValue = builder.extraValue;
        this.requestHeaders = builder.requestHeaders;
        this.requestCookies = builder.requestCookies;
        this.requestBody = builder.requestBody;
        this.requestExtraValue = builder.requestExtraValue;
        this.responseHeaders = builder.responseHeaders;
        this.responseCookies = builder.responseCookies;
        this.responseStatus = builder.responseStatus;
        this.responseExtraValue = builder.responseExtraValue;
        this.errorMessage = builder.errorMessage;
    }

    @Override
    public String toString() {
        return "\nREQUEST:\n" +
                (detailLog ? (logInfo != null ? "\tREQUEST_USER_INFO: " + logInfo + "\n" : "") : "") +
                "\tREQUEST_PATH: " + path + "\n" +
                "\tREQUEST_METHOD: " + method + "\n" +
                (detailLog ? (extraValue != null ? extraValue + "\n" : ""): "") +
                (detailLog ? RemoteConnection.getLogMapValues("REQUEST_HEADERS:", requestHeaders) + "\n" : "") +
                (detailLog ? RemoteConnection.getLogMapValues("REQUEST_COOKIES:", requestCookies) + "\n" : "") +
                (detailLog ? (requestBody != null ? "\tBODY:\n\t\t" + requestBody + "\n" : "") : "") +
                (detailLog ? (requestExtraValue != null ? requestExtraValue + "\n" : "") : "") +
                "RESPONSE:\n" +
                (detailLog ? RemoteConnection.getLogMapValues("RESPONSE_HEADERS:", responseHeaders) + "\n" : "") +
                (detailLog ? RemoteConnection.getLogMapValues("RESPONSE_COOKIES:", responseCookies) + "\n" : "") +
                "\tRESPONSE_STATUS_HTTP: " + (responseStatus != null ? responseStatus : "") + "\n" +
                (detailLog ? (responseExtraValue != null ? responseExtraValue : "") : "") +
                (errorMessage != null ? "\n\tERROR: "  + errorMessage + "\n" : "");
    }

    public static class Builder {
        private boolean detailLog = false;
        private LogInfo logInfo = null;
        private String path = null;
        private String method = null;
        private String extraValue = null;
        private Map<String, String> requestHeaders = null;
        private Map<String, String> requestCookies = null;
        private String requestBody = null;
        private String requestExtraValue = null;
        private Map<String, String> responseHeaders = null;
        private Map<String, String> responseCookies = null;
        private String responseStatus = null;
        private String responseExtraValue = null;
        private String errorMessage = null;

        public Builder() {}

        public Builder detailLog(boolean detailLog) {
            this.detailLog = detailLog;
            return this;
        }

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
        public Builder requestExtraValue (String requestExtraValue) {
            this.requestExtraValue = requestExtraValue;
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
