package lsfusion.base;

import java.io.Serializable;

public class ExecResult implements Serializable {
    public final Object[] results;
    public final String[] headerNames;
    public final String[] headerValues;

    public ExecResult(Object[] results, String[] headerNames, String[] headerValues) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
    }
}
