package lsfusion.interop.session;

import java.io.Serializable;

public class ExternalResponse implements Serializable {
    public final Object[] results;
    
    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public final Integer statusHttp;

    public ExternalResponse(Object[] results, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues, Integer statusHttp) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
        this.statusHttp = statusHttp;
    }
}
