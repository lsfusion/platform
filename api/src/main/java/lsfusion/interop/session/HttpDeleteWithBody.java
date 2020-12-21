package lsfusion.interop.session;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

class HttpDeleteWithBody extends HttpEntityEnclosingRequestBase {
    public static final String METHOD_NAME = "DELETE";
    public String getMethod() { return METHOD_NAME; }

    public HttpDeleteWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }
}