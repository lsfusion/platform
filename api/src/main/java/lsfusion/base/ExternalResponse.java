package lsfusion.base;

import java.io.Serializable;

public class ExternalResponse implements Serializable {
    public final Object[] results;
    
    public final String[] headerNames;
    public final String[] headerValues;
    public final String[] cookieNames;
    public final String[] cookieValues;

    public ExternalResponse(Object[] results, String[] headerNames, String[] headerValues, String[] cookieNames, String[] cookieValues) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.cookieNames = cookieNames;
        this.cookieValues = cookieValues;
    }
}
