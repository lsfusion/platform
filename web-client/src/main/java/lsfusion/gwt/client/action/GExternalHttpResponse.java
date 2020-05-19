package lsfusion.gwt.client.action;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GExternalHttpResponse implements Serializable {
    public String contentType;
    public byte[] responseBytes;
    public Map<String, List<String>> responseHeaders;
    public int statusCode;
    public String statusText;

    @SuppressWarnings("UnusedDeclaration")
    public GExternalHttpResponse() {
    }

    public GExternalHttpResponse(String contentType, byte[] responseBytes, Map<String, List<String>> responseHeaders, int statusCode, String statusText) {
        this.contentType = contentType;
        this.responseBytes = responseBytes;
        this.responseHeaders = responseHeaders;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }


}