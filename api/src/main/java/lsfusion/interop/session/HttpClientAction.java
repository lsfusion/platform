package lsfusion.interop.session;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.http.client.CookieStore;

import java.io.IOException;

public class HttpClientAction implements ClientAction {
    public ExternalHttpMethod method;
    public String connectionString;
    public byte[] body;
    public ImMap<String, String> headers;
    public ImMap<String, String> cookies;
    public CookieStore cookieStore;

    public HttpClientAction(ExternalHttpMethod method, String connectionString, byte[] body, ImMap<String, String> headers, ImMap<String, String> cookies, CookieStore cookieStore) {
        this.method = method;
        this.connectionString = connectionString;
        this.body = body;
        this.headers = headers;
        this.cookies = cookies;
        this.cookieStore = cookieStore;
    }

    @Override
    public ExternalHttpResponse dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return ExternalHttpUtils.sendRequest(method, connectionString, body, headers, cookies, cookieStore);
    }
}