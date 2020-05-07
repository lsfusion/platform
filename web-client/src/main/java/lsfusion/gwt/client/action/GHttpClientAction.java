package lsfusion.gwt.client.action;

import java.util.Map;

public class GHttpClientAction implements GAction {

    public String connectionString;
    public GExternalHttpMethod method;
    public String bodyUrl;
    public String[] paramList;
    public String[] paramTypeList;
    public Map<String, String> headers;

    @SuppressWarnings("UnusedDeclaration")
    public GHttpClientAction() {
    }

    public GHttpClientAction(GExternalHttpMethod method, String connectionString, String bodyUrl, String[] paramList, String[] paramTypeList, Map<String, String> headers) {
        this.method = method;
        this.connectionString = connectionString;
        this.bodyUrl = bodyUrl;
        this.paramList = paramList;
        this.paramTypeList = paramTypeList;
        this.headers = headers;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        return dispatcher.execute(this);
    }
}
