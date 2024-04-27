package lsfusion.interop.session;

public class ResultExternalResponse extends ExternalResponse {

    public final Object[] results;

    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public final int statusHttp;

    @Override
    public int getStatusHttp() {
        return statusHttp;
    }

    public ResultExternalResponse(Object[] results, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, int statusHttp) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
        this.statusHttp = statusHttp;
    }
}
