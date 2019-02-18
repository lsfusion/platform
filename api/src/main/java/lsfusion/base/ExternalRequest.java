package lsfusion.base;

import lsfusion.interop.remote.AuthenticationToken;

import java.io.Serializable;

public class ExternalRequest implements Serializable {

    public final String[] returnNames;
    public final Object[] params;

    public final String charsetName;
    public final String[] headerNames;
    public final String[] headerValues;

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName, String[] headerNames, String[] headerValues) {
        this.returnNames = returnNames;
        this.params = params;
        this.charsetName = charsetName;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
    }
}
