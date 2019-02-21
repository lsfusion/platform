package lsfusion.base;

import java.io.Serializable;

public class ExternalRequest implements Serializable {

    public final String[] returnNames;
    public Object[] params;

    public final String charsetName;
    public final String[] headerNames;
    public final String[] headerValues;

    public final String url;
    public final String query;
    public final String host;
    public final Integer port;
    public final String exportName;

    public ExternalRequest() {
        this(new Object[0]);
    }

    public ExternalRequest(Object[] params) {
        this(new String[0], params);    
    }

    public ExternalRequest(String[] returnNames, Object[] params) {
        this(returnNames, params, "utf-8");
    }

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName) {
        this(returnNames, params, charsetName, null, null, new String[0], new String[0], null, null, null);
    }

    public ExternalRequest(String[] returnNames, Object[] params, String charsetName, String url, String query, String[] headerNames,
                           String[] headerValues, String host, Integer port, String exportName) {
        this.returnNames = returnNames;
        this.params = params;
        this.charsetName = charsetName;
        this.headerNames = headerNames;
        this.headerValues = headerValues;
        this.url = url;
        this.query = query;
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }
}
