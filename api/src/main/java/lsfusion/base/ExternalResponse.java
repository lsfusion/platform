package lsfusion.base;

import java.io.Serializable;

public class ExternalResponse implements Serializable {
    public final Object[] results;
    
    public final String[] headerNames;
    public final String[] headerValues;

    public ExternalResponse(Object[] results, String[] headerNames, String[] headerValues) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
    }
}
