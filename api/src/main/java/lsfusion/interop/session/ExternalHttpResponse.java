package lsfusion.interop.session;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class ExternalHttpResponse implements Serializable {
    public String contentType;
    public byte[] responseBytes;
    public Map<String, List<String>> responseHeaders;
    public int statusCode;
    public String statusText;

    public ExternalHttpResponse(String contentType, byte[] responseBytes, Map<String, List<String>> responseHeaders, int statusCode, String statusText) {
        this.contentType = contentType;
        this.responseBytes = responseBytes;
        this.responseHeaders = responseHeaders;
        this.statusCode = statusCode;
        this.statusText = statusText;
    }
}