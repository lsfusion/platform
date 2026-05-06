package lsfusion.interop.session;

public class ResultExternalResponse extends ExternalResponse {

    public final ExternalRequest.Result[] results;

    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public final int statusHttp;

    /**
     * Server-collected {@code MESSAGE} / {@code PRINT MESSAGE} text emitted by the action,
     * each prefixed with its {@link lsfusion.interop.action.MessageClientType} (e.g.
     * {@code "WARN: hello"}, {@code "ERROR: ..."}). Always populated by
     * {@code RemoteConnection.executeExternal} on the non-interactive path; consumers that
     * don't care (e.g. {@code /eval} / {@code /exec} HTTP handlers) just ignore the field.
     */
    public final String[] logMessages;

    @Override
    public int getStatusHttp() {
        return statusHttp;
    }

    public ResultExternalResponse(ExternalRequest.Result[] results, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, int statusHttp) {
        this(results, headerNames, headerValues, cookieNames, cookieValues, statusHttp, null);
    }

    public ResultExternalResponse(ExternalRequest.Result[] results, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, int statusHttp, String[] logMessages) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
        this.statusHttp = statusHttp;
        this.logMessages = logMessages;
    }
}
