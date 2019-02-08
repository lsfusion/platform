package lsfusion.base;

public class ExecResult {
    public final Object[] results;
    public final String[] headerNames;
    public final String[] headerValues;

    public ExecResult(Object[] results, String[] headerNames, String[] headerValues) {
        this.results = results;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
    }
}
