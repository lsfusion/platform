package lsfusion.gwt.client.action;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

import java.util.HashMap;

public class GHttpClientAction implements GAction {

    public String connectionString;
    public GExternalHttpMethod method;
    public String bodyUrl;
    /*Object[] paramList;
    ImMap<String, String> headers;
    ImMap<String, String> cookies;
    CookieStore cookieStore;*/

    @SuppressWarnings("UnusedDeclaration")
    public GHttpClientAction() {
    }

    public GHttpClientAction(GExternalHttpMethod method, String connectionString, String bodyUrl) {
        this.method = method;
        this.connectionString = connectionString;
        this.bodyUrl = bodyUrl;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
