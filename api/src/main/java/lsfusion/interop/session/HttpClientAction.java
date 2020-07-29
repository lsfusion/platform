package lsfusion.interop.session;

import lsfusion.interop.action.ClientAction;
import lsfusion.interop.action.ClientActionDispatcher;
import org.apache.http.client.CookieStore;

import java.io.IOException;
import java.util.Map;

public class HttpClientAction implements ClientAction {
    public ExternalHttpMethod method;
    public String connectionString;
    public Integer timeout;
    public byte[] body;
    public Map<String, String> headers;
    public Map<String, String> cookies;
    public CookieStore cookieStore;

    public HttpClientAction(ExternalHttpMethod method, String connectionString, Integer timeout, byte[] body,
                            Map<String, String> headers, Map<String, String> cookies, CookieStore cookieStore) {
        this.method = method;
        this.connectionString = connectionString;
        this.timeout = timeout;
        this.body = body;
        this.headers = headers;
        this.cookies = cookies;
        this.cookieStore = cookieStore;
    }

    @Override
    public ExternalHttpResponse dispatch(ClientActionDispatcher dispatcher) throws IOException {
        return ExternalHttpUtils.sendRequest(method, connectionString, timeout, body, headers, cookies, cookieStore);
    }
}